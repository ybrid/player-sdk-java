/*
 * Copyright (c) 2021 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
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

package io.ybrid.player.io.codec.opus.implementation;

import io.ybrid.api.PlayoutInfo;
import io.ybrid.api.metadata.Sync;
import io.ybrid.player.io.ByteDataBlock;
import io.ybrid.player.io.audio.PCMDataBlock;
import io.ybrid.player.io.audio.PCMDataSource;
import io.ybrid.player.io.audio.Skipper;
import io.ybrid.player.io.decoder.StreamDecoder;
import io.ybrid.player.io.mapping.Header;
import io.ybrid.player.io.mapping.ogg.opus.OpusDataBlock;
import io.ybrid.player.io.mapping.ogg.opus.OpusHead;
import io.ybrid.player.io.muxer.Stream;
import io.ybrid.player.util.LazyClass;
import io.ybrid.player.util.LazyObject;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class LazyLoadingAndroidDecoder extends StreamDecoder<Stream<?, ?, ? extends ByteDataBlock, ?>> {
    static final @NonNls Logger LOGGER = Logger.getLogger(LazyLoadingAndroidDecoder.class.getName());

    private static final LazyClass MediaFormat = new LazyClass("android.media.MediaFormat");
    private static final LazyClass MediaCodec = new LazyClass("android.media.MediaCodec");
    private static final LazyClass MediaCodecList = new LazyClass("android.media.MediaCodecList");
    private static final LazyClass BufferInfo = new LazyClass("android.media.MediaCodec$BufferInfo");
    private static final int BUFFER_FLAG_END_OF_STREAM = MediaCodec.getIntField("BUFFER_FLAG_END_OF_STREAM");

    private interface Reader {
        @NotNull PCMDataBlock read() throws IOException;
    }

    private static class OpusPCMDataSource implements PCMDataSource {
        final @NotNull Reader reader;
        final @NotNull Closeable closeable;
        final @NotNull Supplier<Boolean> valid;

        public OpusPCMDataSource(@NotNull Reader reader, @NotNull Closeable closeable, @NotNull Supplier<Boolean> valid) {
            this.reader = reader;
            this.closeable = closeable;
            this.valid = valid;
        }

        @Override
        public @NotNull PCMDataBlock read() throws IOException {
            return reader.read();
        }

        @Override
        public boolean isValid() {
            return valid.get();
        }

        @Override
        public void close() throws IOException {
            closeable.close();
        }
    }

    private static class OpusSkipper extends Skipper<PCMDataSource> {
        public OpusSkipper(@NotNull PCMDataSource backend) {
            super(backend);
        }

        @Override
        protected void examine(@NotNull PCMDataBlock block) {
            // no-op.
        }

        public void setPostSkip(long postSkip) {
            this.postSkip = postSkip;
        }
    }

    private final LazyObject bufferInfo = BufferInfo.newInstance();
    private final @NotNull OpusSkipper skipper;
    private boolean valid = true;
    private Sync sync;
    private PlayoutInfo playoutInfo;
    private long presentationTimeUs = 0;
    private LazyObject decoder;
    private Method dequeueInputBuffer;
    private Method queueInputBuffer;

    public static void assertAvailable() {
        /* noop */
        LOGGER.info("LazyLoadingAndroidDecoder is available");
    }

    public LazyLoadingAndroidDecoder(@NotNull Stream<?, ?, ? extends ByteDataBlock, ?> stream) {
        super(stream);
        //this.skipper = new OpusSkipper(new OpusPCMDataSource(this::readInternal, this::closeInternal, this::isValidInternal));
        this.skipper = new OpusSkipper(new OpusPCMDataSource(this::readInternal, this::closeInternal, this::isValidInternal));
        LOGGER.info("Created new instance of LazyLoadingAndroidDecoder for stream " + stream);
    }

    static @NotNull ByteBuffer longToBuffer(long val) {
        final @NotNull ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.nativeOrder());
        buffer.putLong(val);
        buffer.position(0);
        return buffer;
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private void openDecoder() {
        final @NotNull LazyObject mediaCodecList = MediaCodecList.newInstance(MediaCodecList.findConstructor(Integer.TYPE), MediaCodecList.getIntField("REGULAR_CODECS"));
        final @NotNull List<? extends Header> headers = stream.getHeaders();
        final @NotNull LazyObject format;
        final @NotNull OpusHead opusHead = (OpusHead) headers.get(0);
        final @NotNull Method setByteBuffer;

        format = new LazyObject(Objects.requireNonNull(MediaFormat.invoke(
                MediaFormat.findMethod("createAudioFormat", String.class, Integer.TYPE, Integer.TYPE),
                MediaFormat.getField("MIMETYPE_AUDIO_OPUS"), 48000, opusHead.getChannelCount()
        )));

        setByteBuffer = format.findMethod("setByteBuffer", String.class, ByteBuffer.class);

        format.invoke(setByteBuffer, "csd-0", ByteBuffer.wrap(opusHead.getRaw()));
        format.invoke(setByteBuffer, "csd-1", longToBuffer((long)(opusHead.getPreSkip() * 1000000000. / 48000.)));
        format.invoke(setByteBuffer, "csd-2", longToBuffer(80000000)); // 80ms.

        decoder = new LazyObject(Objects.requireNonNull(
                MediaCodec.invoke(MediaCodec.findMethod("createByCodecName", String.class),
                        mediaCodecList.invoke(mediaCodecList.findMethod("findDecoderForFormat", MediaFormat.getRawClass()), format.getObject())
                )
        ));
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

    private void pumpInputIn(@NotNull ByteDataBlock block, int idx) {
        final @NotNull ByteBuffer buffer = (ByteBuffer) Objects.requireNonNull(decoder.invoke(decoder.findMethod("getInputBuffer", Integer.TYPE), idx));
        final @NotNull OpusDataBlock opusDataBlock = (OpusDataBlock) block;

        sync = block.getSync();
        playoutInfo = block.getPlayoutInfo();

        buffer.clear();
        buffer.put(block.getData());
        buffer.position(0);

        if (opusDataBlock.getData().length > 0) {
            presentationTimeUs = opusDataBlock.getGranularPosition().subtract(Objects.requireNonNull(opusDataBlock.getTableOfContents()).getAudioFrameCount()).get(1000000, 48000);
        }

        decoder.invoke(queueInputBuffer, idx, 0, block.getData().length, presentationTimeUs, 0);
    }

    private void pumpInput() {
        if (decoder == null) {
            final @NotNull ByteDataBlock block;
            try {
                block = stream.read();
            } catch (IOException e) {
                valid = false;
                return;
            }
            openDecoder();
            {
                final int idx = decoder.invokeInt(dequeueInputBuffer, 1000);
                pumpInputIn(block, idx);
            }
        } else {
            final int idx = decoder.invokeInt(dequeueInputBuffer, 1000);
            if (idx >= 0) {
                try {
                    final @NotNull ByteDataBlock block = stream.read();
                    pumpInputIn(block, idx);
                } catch (IOException e) {
                    decoder.invoke(queueInputBuffer, idx, 0, 0, 0, BUFFER_FLAG_END_OF_STREAM);
                }
            }
        }
    }

    private @NotNull PCMDataBlock readInternal() throws IOException {
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
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

    private boolean isValidInternal() {
        return valid && (bufferInfo.getIntField("flags") & BUFFER_FLAG_END_OF_STREAM) == 0;
    }

    private void closeInternal() throws IOException {
        if (!valid)
            return;

        if (decoder != null) {
            decoder.invoke("stop");
            decoder.invoke("release");
            decoder = null;
        }

        stream.close();

        valid = false;
    }

    @Override
    @NotNull
    public PCMDataBlock read() throws IOException {
        return skipper.read();
    }

    @Override
    public boolean isValid() {
        return skipper.isValid();
    }

    @Override
    public void close() throws IOException {
        skipper.close();
    }
}
