/*
 * Copyright (c) 2022 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.ybrid.player.io.codec.aac;

import io.ybrid.api.PlayoutInfo;
import io.ybrid.api.metadata.Sync;
import io.ybrid.player.io.ByteDataBlock;
import io.ybrid.player.io.ByteDataSource;
import io.ybrid.player.io.audio.PCMDataBlock;
import io.ybrid.player.io.container.adts.Frame;
import io.ybrid.player.io.container.adts.Header;
import io.ybrid.player.util.LazyClass;
import io.ybrid.player.util.LazyObject;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Objects;
import java.util.logging.Logger;

public class LazyLoadingAndroidDecoder implements io.ybrid.player.io.decoder.Decoder {
    static final @NonNls Logger LOGGER = Logger.getLogger(LazyLoadingAndroidDecoder.class.getName());

    private static final LazyClass MediaFormat = new LazyClass("android.media.MediaFormat");
    private static final LazyClass MediaCodec = new LazyClass("android.media.MediaCodec");
    private static final LazyClass MediaCodecList = new LazyClass("android.media.MediaCodecList");
    private static final LazyClass BufferInfo = new LazyClass("android.media.MediaCodec$BufferInfo");
    private static final int BUFFER_FLAG_END_OF_STREAM = MediaCodec.getIntField("BUFFER_FLAG_END_OF_STREAM");

    private final @NotNull io.ybrid.player.io.container.adts.Sync dataSource;
    private final LazyObject bufferInfo = BufferInfo.newInstance();
    private long presentationTimeUs = 0;
    private boolean valid = true;
    private Sync sync;
    private PlayoutInfo playoutInfo;
    private LazyObject decoder;
    private Method dequeueInputBuffer;
    private Method queueInputBuffer;

    public LazyLoadingAndroidDecoder(@NotNull ByteDataSource dataSource) {
        LOGGER.info("Trying LazyLoadingAndroidDecoder for AAC");
        this.dataSource = new io.ybrid.player.io.container.adts.Sync(dataSource);
    }

    private @NotNull ByteBuffer createCSD0(@NotNull Frame frame) {
        final @NotNull ByteBuffer csd = ByteBuffer.allocate(2);
        final @NotNull Header header = frame.getHeader();
        final int profile = header.getAudioObjectType().toWire();
        final int sampleRate = header.getSamplingFrequency().toWire();
        final int channel = header.getChannelConfiguration().toWire();

        LOGGER.info("CSD-0 Buffer: profile=" + profile + ", sampleRate=" + sampleRate + ", channel=" + channel);

        csd.put(0, (byte)( ((profile + 1) << 3) | sampleRate >> 1 ) );
        csd.put(1, (byte)( ((sampleRate << 7) & 0x80) | channel << 3 ) );

        return csd;
    }

    private void openDecoder(@NotNull Frame frame) {
        final @NotNull LazyObject mediaCodecList = MediaCodecList.newInstance(MediaCodecList.findConstructor(Integer.TYPE), MediaCodecList.getIntField("REGULAR_CODECS"));
        final @NotNull Header header = frame.getHeader();
        final @NotNull LazyObject format;

        format = new LazyObject(Objects.requireNonNull(MediaFormat.invoke(
                MediaFormat.findMethod("createAudioFormat", String.class, Integer.TYPE, Integer.TYPE),
                MediaFormat.getField("MIMETYPE_AUDIO_AAC"), header.getSamplingFrequency().getFrequency(), header.getChannelConfiguration().getCount()
        )));

        format.invoke(format.findMethod("setInteger", String.class, Integer.TYPE), format.getField("KEY_IS_ADTS"), 1);

        format.invoke(
                format.findMethod("setByteBuffer", String.class, ByteBuffer.class),
                "csd-0",
                createCSD0(frame)
        );

        LOGGER.info("Audio format: " + format.getObject());

        decoder = new LazyObject(Objects.requireNonNull(
                MediaCodec.invoke(MediaCodec.findMethod("createByCodecName", String.class),
                        mediaCodecList.invoke(mediaCodecList.findMethod("findDecoderForFormat", MediaFormat.getRawClass()), format.getObject())
                )
        ));

        LOGGER.info("Got decoder: " + decoder);

        decoder.invoke(decoder.findMethod("configure",
                MediaFormat.getRawClass(),
                new LazyClass("android.view.Surface").getRawClass(),
                new LazyClass("android.media.MediaCrypto").getRawClass(),
                Integer.TYPE
        ), format.getObject(), null, null, 0);
        decoder.invoke("start");

        dequeueInputBuffer = decoder.findMethod("dequeueInputBuffer", Long.TYPE);
        queueInputBuffer = decoder.findMethod("queueInputBuffer", Integer.TYPE, Integer.TYPE, Integer.TYPE, Long.TYPE, Integer.TYPE);
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void close() throws IOException {
        valid = false;
    }


    private void pumpInputIn(@NotNull ByteDataBlock block, int idx) {
        final @NotNull ByteBuffer buffer = (ByteBuffer) Objects.requireNonNull(decoder.invoke(decoder.findMethod("getInputBuffer", Integer.TYPE), idx));
        final @NotNull Frame frame = (Frame) block;

        sync = block.getSync();
        playoutInfo = block.getPlayoutInfo();

        buffer.clear();
        buffer.put(block.getData());
        buffer.position(0);

        decoder.invoke(queueInputBuffer, idx, 0, block.getData().length, presentationTimeUs, 0);

        if (block.getData().length > 0) {
            presentationTimeUs += (frame.getLengthAsFrames() * 1_000_000L) / frame.getSampleRate();
        }
    }

    private void pumpInput() {
        if (decoder == null) {
            final @NotNull ByteDataBlock block;
            try {
                block = dataSource.read();
            } catch (IOException e) {
                valid = false;
                return;
            }
            try {
                openDecoder((Frame) block);
                LOGGER.info("Decoder opened: " + decoder + " -> " + decoder.getObject());
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }
            {
                final int idx = decoder.invokeInt(dequeueInputBuffer, 1000);
                pumpInputIn(block, idx);
            }
        } else {
            final int idx = decoder.invokeInt(dequeueInputBuffer, 1000);
            if (idx >= 0) {
                try {
                    final @NotNull ByteDataBlock block = dataSource.read();
                    pumpInputIn(block, idx);
                } catch (IOException e) {
                    decoder.invoke(queueInputBuffer, idx, 0, 0, 0, BUFFER_FLAG_END_OF_STREAM);
                }
            }
        }
    }

    @Override
    public @NotNull PCMDataBlock read() throws IOException {
        if (!valid)
            throw new IOException("No longer valid.");

        try {
            if (decoder == null)
                pumpInput();

            while (true) {
                // output:
                {
                    final int idx = decoder.invokeInt(decoder.findMethod("dequeueOutputBuffer", BufferInfo.getRawClass(), Long.TYPE), bufferInfo.getObject(), 1);

                    if (idx >= 0) {
                        final @NotNull ByteBuffer buffer = (ByteBuffer) Objects.requireNonNull(decoder.invoke(decoder.findMethod("getOutputBuffer", Integer.TYPE), idx));
                        final @NotNull LazyObject outputFormat = new LazyObject(Objects.requireNonNull(decoder.invoke(decoder.findMethod("getOutputFormat", Integer.TYPE), idx)));
                        final @NotNull Method getInteger = outputFormat.findMethod("getInteger", String.class);
                        final @NotNull ShortBuffer samples = buffer.order(ByteOrder.nativeOrder()).asShortBuffer();
                        final short[] pcm = new short[samples.remaining()];

                        samples.get(pcm);

                        decoder.invoke(decoder.findMethod("releaseOutputBuffer", Integer.TYPE, Boolean.TYPE), idx, false);

                        return new PCMDataBlock(sync,
                                playoutInfo,
                                pcm,
                                outputFormat.invokeInt(getInteger, MediaFormat.getField("KEY_SAMPLE_RATE")),
                                outputFormat.invokeInt(getInteger, MediaFormat.getField("KEY_CHANNEL_COUNT")));
                    }
                }

                if ((bufferInfo.getIntField("flags") & BUFFER_FLAG_END_OF_STREAM) != 0)
                    throw new EOFException();

                pumpInput();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }
}
