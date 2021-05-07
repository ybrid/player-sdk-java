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

package io.ybrid.player.player;

import io.ybrid.api.*;
import io.ybrid.api.bouquet.Bouquet;
import io.ybrid.api.bouquet.Service;
import io.ybrid.api.metadata.ItemType;
import io.ybrid.api.session.Command;
import io.ybrid.api.transaction.Request;
import io.ybrid.api.transaction.Transaction;
import io.ybrid.api.transaction.TransactionExecutionException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;

/**
 * This interface is implemented by objects that control a session.
 */
public interface MediaController extends Player, KnowsSubInfoState {
    /* --- Object Status --- */

    /**
     * Returns whether this Session is valid.
     * If the Session is invalid the client must no longer use it and create a new session if necessary.
     * @return Returns validity of this Session.
     */
    @ApiStatus.Experimental
    boolean isValid();


    /* --- Getters --- */

    /**
     * Get the current set of {@link Capability Capabilities} supported.
     * This can be used to display different options to the user.
     *
     * Calling this resets the flag returned by {@link KnowsSubInfoState#hasChanged(SubInfo)}
     * @return Returns the set of current {@link Capability Capabilities}.
     */
    @ApiStatus.Experimental
    @NotNull
    CapabilitySet getCapabilities();

    /**
     * Get the current Bouquet of active Services.
     * The Bouquet may be displayed to the user to select the Service to listen to.
     *
     * Calling this resets the flag returned by {@link KnowsSubInfoState#hasChanged(SubInfo)}
     * @return Returns the current Bouquet.
     */
    @ApiStatus.Experimental
    @NotNull
    Bouquet getBouquet();

    /**
     * Get the current {@link PlayoutInfo} for the session.
     *
     * Calling this resets the flag returned by {@link KnowsSubInfoState#hasChanged(SubInfo)}
     * @return Returns the current {@link PlayoutInfo}.
     */
    @ApiStatus.Experimental
    @NotNull
    PlayoutInfo getPlayoutInfo();


    /* --- Actions --- */

    /**
     * This call requests the session to be brought back to the live portion of the current service.
     * <p>
     * The default implementation makes use of {@link #execute(Request)}.
     *
     * @throws TransactionExecutionException Thrown if a transaction failed while this method was still executing.
     */
    default  @NotNull Transaction windToLive() throws TransactionExecutionException {
        return execute(Command.WIND_TO_LIVE.makeRequest());
    }

    /**
     * This call requests the session to be brought to the given time within the current service.
     * <p>
     * The default implementation makes use of {@link #execute(Request)}.
     *
     * @param timestamp The timestamp to jump to.
     * @throws TransactionExecutionException Thrown if a transaction failed while this method was still executing.
     */
    default @NotNull Transaction windTo(@NotNull Instant timestamp) throws TransactionExecutionException {
        return execute(Command.WIND_TO.makeRequest(timestamp));
    }

    /**
     * This call allows to move in the stream by a relative time.
     * The time can be positive to move into the future or negative to move into the past
     * relative to the current position.
     * <p>
     * The default implementation makes use of {@link #execute(Request)}.
     *
     * @param duration The duration to wind.
     * @throws TransactionExecutionException Thrown if a transaction failed while this method was still executing.
     */
    default @NotNull Transaction wind(@NotNull Duration duration) throws TransactionExecutionException {
        return execute(Command.WIND_BY.makeRequest(duration));
    }

    /**
     * Skip to the next Item of the given type.
     * <p>
     * The default implementation makes use of {@link #execute(Request)}.
     *
     * @param itemType The ItemType to skip to.
     * @throws TransactionExecutionException Thrown if a transaction failed while this method was still executing.
     */
    default @NotNull Transaction skipForwards(ItemType itemType) throws TransactionExecutionException {
        return execute(Command.SKIP_FORWARD.makeRequest(itemType));
    }

    /**
     * Skip to the previous Item of the given type.
     * <p>
     * The default implementation makes use of {@link #execute(Request)}.
     *
     * @param itemType The ItemType to skip to.
     * @throws TransactionExecutionException Thrown if a transaction failed while this method was still executing.
     */
    default @NotNull Transaction skipBackwards(ItemType itemType) throws TransactionExecutionException {
        return execute(Command.SKIP_BACKWARD.makeRequest(itemType));
    }

    /**
     * Swap the current Item with a different one.
     * <p>
     * The default implementation makes use of {@link #execute(Request)}.
     *
     * @param mode The mode for the swap. See {@link SwapMode} for details.
     * @throws TransactionExecutionException Thrown if a transaction failed while this method was still executing.
     */
    default @NotNull Transaction swapItem(SwapMode mode) throws TransactionExecutionException {
        return execute(Command.SWAP_ITEM.makeRequest(mode));
    }

    /**
     * This call requests the session to be brought back to the main service of this bouquet.
     * <p>
     * The default implementation makes use of {@link #execute(Request)}.
     *
     * @throws TransactionExecutionException Thrown if a transaction failed while this method was still executing.
     */
    default @NotNull Transaction swapToMain() throws TransactionExecutionException {
        return execute(Command.SWAP_TO_MAIN_SERVICE.makeRequest());
    }

    /**
     * Swap to a different Service.
     * <p>
     * The default implementation makes use of {@link #execute(Request)}.
     *
     * @param service The new service to listen to.
     * @throws TransactionExecutionException Thrown if a transaction failed while this method was still executing.
     */
    default @NotNull Transaction swapService(@NotNull Service service) throws TransactionExecutionException {
        return execute(Command.SWAP_SERVICE.makeRequest(service));
    }
}
