/*
 * Copyright (c) 2020 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
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

package io.ybrid.player.io.mapping.ogg.opus;

import io.ybrid.api.metadata.Sync;
import io.ybrid.api.metadata.source.Source;
import io.ybrid.api.metadata.source.SourceType;
import io.ybrid.api.metadata.source.VorbisCommentBasedMetadata;
import io.ybrid.player.io.DataBlock;
import io.ybrid.player.io.container.ogg.Flag;
import io.ybrid.player.io.container.ogg.GranularPosition;
import io.ybrid.player.io.container.ogg.Page;
import io.ybrid.player.io.mapping.ogg.Generic;
import io.ybrid.player.io.muxer.StreamInfo;
import io.ybrid.player.io.muxer.StreamUsage;
import io.ybrid.player.io.muxer.ogg.PacketAdapter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

public class Mapping extends Generic {
    static final @NonNls Logger LOGGER = Logger.getLogger(Mapping.class.getName());

    private final @NotNull Source source = new Source(SourceType.FORMAT);
    private @Nullable OpusHead opusHead = null;
    private @Nullable OpusTags opusTags = null;
    private @NotNull GranularPosition granularPosition = GranularPosition.INVALID;
    private @Nullable Deque<OpusDataBlock> stack = new ArrayDeque<>();
    private @Nullable Sync lastInputSync;
    private @Nullable Sync lastOutputSync;

    private void processStack(@NotNull OpusDataBlock block) {
        @NotNull GranularPosition granularPosition;

        if (stack == null)
            return;

        granularPosition = block.getGranularPosition();
        if (granularPosition.isValid()) {
            try {
                granularPosition = granularPosition.subtract(block.getTableOfContents().getAudioFrameCount());
                for (Iterator<OpusDataBlock> iterator = stack.descendingIterator(); iterator.hasNext(); ) {
                    OpusDataBlock e = iterator.next();
                    e.setGranularPosition(granularPosition);
                    granularPosition = granularPosition.subtract(block.getTableOfContents().getAudioFrameCount());
                }
            } catch (Throwable ignored) {
            }
            stack = null;
        } else {
            stack.addLast(block);
        }
    }

    private @NotNull Sync getSync(final @NotNull Sync inputSync) {
        final @NotNull Sync.Builder builder;

        if (lastOutputSync != null && inputSync.equals(lastInputSync))
            return lastOutputSync;

        if (opusTags == null)
            throw new NullPointerException("No OpusTags found. BAD.");

        lastInputSync = inputSync;

        if (lastOutputSync == null) {
            builder = new Sync.Builder(source, inputSync);
        } else {
            builder = new Sync.Builder(inputSync, lastOutputSync);
        }
        builder.autoFill();
        builder.setCurrentTrack(new VorbisCommentBasedMetadata(source, opusTags.getVendorString(), opusTags.getComments()));
        lastOutputSync = builder.build();

        return lastOutputSync;
    }

    @Override
    public @NotNull DataBlock process(@NotNull PacketAdapter block) {
        if (Header.isHeader(block, OpusHead.MAGIC)) {
            opusHead = new OpusHead(block);
            return opusHead;
        } else if (Header.isHeader(block, OpusTags.MAGIC)) {
            opusTags = new OpusTags(block);
            return opusTags;
        } else {
            final @NotNull OpusDataBlock ret;
            final @NotNull GranularPosition fromBlock = block.getPacket().getGranularPosition();

            if (fromBlock.isValid()) {
                if (granularPosition.isValid()) {
                    if (!granularPosition.equals(fromBlock)) {
                        if (!(block.getPacket().getFlags().contains(Flag.EOS) && fromBlock.isLessOrEqualThan(granularPosition))) {
                            LOGGER.severe("Jump in granularPosition from " + granularPosition + " to " + fromBlock);
                        }
                    }
                }
                granularPosition = fromBlock;
            }

            ret = new OpusDataBlock(Objects.requireNonNull(opusHead), getSync(block.getSync()), block, granularPosition);
            processStack(ret);
            granularPosition = granularPosition.add(ret.getTableOfContents().getAudioFrameCount());
            return ret;
        }
    }

    @Override
    public @NotNull StreamUsage getPrimaryStreamUsage() {
        return StreamUsage.AUDIO;
    }

    @Override
    public @NotNull Set<StreamUsage> getStreamUsage() {
        return EnumSet.of(StreamUsage.AUDIO, StreamUsage.METADATA);
    }

    public static @Nullable StreamInfo test(@NotNull Page page) {
        if (page.getBody().length > 8 && page.bodyContains(0, OpusHead.MAGIC)) {
            return new StreamInfo(new Mapping());
        }
        return null;
    }

    @Override
    public io.ybrid.api.util.@Nullable MediaType getMediaType() {
        return io.ybrid.player.io.MediaType.BLOCK_STREAM_OPUS;
    }
}
