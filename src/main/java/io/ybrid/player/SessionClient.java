/*
 * Copyright (c) 2019 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
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

package io.ybrid.player;

import io.ybrid.api.*;
import io.ybrid.api.bouquet.Bouquet;
import io.ybrid.api.bouquet.Service;
import io.ybrid.api.metadata.ItemType;
import io.ybrid.player.player.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * This interface is implemented by objects that control a session.
 */
public interface SessionClient extends Player, KnowsSubInfoState {
    /* --- Object Status --- */

    /**
     * Returns whether this Session is valid.
     * If the Session is invalid the client must no longer use it and create a new session if necessary.
     * @return Returns validity of this Session.
     */
    boolean isValid();


    /* --- Getters --- */

    /**
     * Get the current set of {@link Capability Capabilities} supported.
     * This can be used to display different options to the user.
     *
     * Calling this resets the flag returned by {@link KnowsSubInfoState#hasChanged(SubInfo)}
     * @return Returns the set of current {@link Capability Capabilities}.
     */
    @NotNull
    CapabilitySet getCapabilities();

    /**
     * Get the current Bouquet of active Services.
     * The Bouquet may be displayed to the user to select the Service to listen to.
     *
     * Calling this resets the flag returned by {@link KnowsSubInfoState#hasChanged(SubInfo)}
     * @return Returns the current Bouquet.
     */
    @NotNull
    Bouquet getBouquet();

    /**
     * Get the current {@link PlayoutInfo} for the session.
     *
     * Calling this resets the flag returned by {@link KnowsSubInfoState#hasChanged(SubInfo)}
     * @return Returns the current {@link PlayoutInfo}.
     */
    @NotNull
    PlayoutInfo getPlayoutInfo();


    /* --- Actions --- */

    /**
     * This call requests the session to be brought back to the live portion of the current service.
     * @throws IOException Thrown on any I/O-Error.
     */
    void windToLive() throws IOException;

    /**
     * This call requests the session to be brought to the given time within the current service.
     * @param timestamp The timestamp to jump to.
     * @throws IOException Thrown on any I/O-Error.
     */
    void windTo(@NotNull Instant timestamp) throws IOException;

    /**
     * This call allows to move in the stream by a relative time.
     * The time can be positive to move into the future or negative to move into the past
     * relative to the current position.
     * @param duration The duration to wind.
     * @throws IOException Thrown on any I/O-Error.
     */
    void wind(@NotNull Duration duration) throws IOException;

    /**
     * Skip to the next Item of the given type.
     * @param itemType The ItemType to skip to.
     * @throws IOException Thrown on any I/O-Error.
     */
    void skipForwards(ItemType itemType) throws IOException;

    /**
     * Skip to the previous Item of the given type.
     * @param itemType The ItemType to skip to.
     * @throws IOException Thrown on any I/O-Error.
     */
    void skipBackwards(ItemType itemType) throws IOException;

    /**
     * Swap the current Item with a different one.
     * @param mode The mode for the swap. See {@link SwapMode} for details.
     * @throws IOException Thrown on any I/O-Error.
     */
    void swapItem(SwapMode mode) throws IOException;

    /**
     * This call requests the session to be brought back to the main service of this bouquet.
     *
     * @throws IOException Thrown on any I/O-Error.
     */
    default void swapToMain() throws IOException {
        swapService(getBouquet().getDefaultService());
    }

    /**
     * Swap to a different Service.
     * @param service The new service to listen to.
     */
    void swapService(@NotNull Service service) throws IOException;
}
