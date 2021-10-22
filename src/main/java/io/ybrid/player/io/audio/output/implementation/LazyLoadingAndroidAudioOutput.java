package io.ybrid.player.io.audio.output.implementation;

import io.ybrid.player.io.audio.PCMDataBlock;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

@SuppressWarnings("HardCodedStringLiteral")
@ApiStatus.Internal
public class LazyLoadingAndroidAudioOutput extends Base {
    /**
     * This configures how many times the minimal buffer should be used for playback.
     */
    private static final int BUFFER_SCALE = 4;

    private static final @NotNull ClassLoader LOADER = Objects.requireNonNull(LazyLoadingAndroidAudioOutput.class.getClassLoader());
    private static final @NotNull Class<?> CLASS_AUDIO_MANAGER;
    private static final @NotNull Class<?> CLASS_AUDIO_ATTRIBUTES;
    private static final @NotNull Class<?> CLASS_AUDIO_ATTRIBUTES_BUILDER;
    private static final @NotNull Class<?> CLASS_AUDIO_FORMAT;
    private static final @NotNull Class<?> CLASS_AUDIO_FORMAT_BUILDER;
    private static final @NotNull Class<?> CLASS_AUDIO_TRACK;
    private static final @NotNull Method METHOD_AUDIO_TRACK_WRITE;
    private static final @NotNull Method METHOD_AUDIO_TRACK_PLAY;
    private static final @NotNull Method METHOD_AUDIO_TRACK_FLUSH;
    private static final @NotNull Method METHOD_AUDIO_TRACK_STOP;
    private static final @NotNull Method METHOD_AUDIO_TRACK_RELEASE;
    private static final int CHANNEL_OUT_MONO;
    private static final int CHANNEL_OUT_STEREO;
    private static final int ENCODING_PCM_16BIT;

    static {
        try {
            CLASS_AUDIO_MANAGER = LOADER.loadClass("android.media.AudioManager");
            CLASS_AUDIO_ATTRIBUTES = LOADER.loadClass("android.media.AudioAttributes");
            CLASS_AUDIO_ATTRIBUTES_BUILDER = LOADER.loadClass("android.media.AudioAttributes$Builder");
            CLASS_AUDIO_FORMAT = LOADER.loadClass("android.media.AudioFormat");
            CLASS_AUDIO_FORMAT_BUILDER = LOADER.loadClass("android.media.AudioFormat$Builder");
            CLASS_AUDIO_TRACK = LOADER.loadClass("android.media.AudioTrack");
            METHOD_AUDIO_TRACK_WRITE = CLASS_AUDIO_TRACK.getMethod("write", short[].class, Integer.TYPE, Integer.TYPE);
            METHOD_AUDIO_TRACK_PLAY = CLASS_AUDIO_TRACK.getMethod("play");
            METHOD_AUDIO_TRACK_FLUSH = CLASS_AUDIO_TRACK.getMethod("flush");
            METHOD_AUDIO_TRACK_STOP = CLASS_AUDIO_TRACK.getMethod("stop");
            METHOD_AUDIO_TRACK_RELEASE = CLASS_AUDIO_TRACK.getMethod("release");
            CHANNEL_OUT_MONO = getIntField(CLASS_AUDIO_FORMAT, "CHANNEL_OUT_MONO");
            CHANNEL_OUT_STEREO = getIntField(CLASS_AUDIO_FORMAT, "CHANNEL_OUT_STEREO");
            ENCODING_PCM_16BIT = getIntField(CLASS_AUDIO_FORMAT, "ENCODING_PCM_16BIT");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Object invoke(@NotNull Method method, @Nullable Object obj, Object... args) {
        try {
            return method.invoke(obj, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static void invokeSetter(@NotNull @NonNls String method, @NotNull Object obj, int arg) {
        try {
            invoke(obj.getClass().getMethod(method, Integer.TYPE), obj, arg);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object invokeBuild(@NotNull Object obj) {
        try {
            return invoke(obj.getClass().getMethod("build"), obj);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object getField(@NotNull Class<?> clazz, @NotNull @NonNls String name) {
        try {
            return Objects.requireNonNull(Objects.requireNonNull(clazz.getField(name)).get(null));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private static int getIntField(@NotNull Class<?> clazz, @NotNull @NonNls String name) {
        return (int)getField(clazz, name);
    }

    private @Nullable Object audioTrack;

    @Override
    protected void configureBackend(@NotNull PCMDataBlock block) throws IOException {
        try {
            final int sampleRate = block.getSampleRate();
            final int channels = block.getNumberOfChannels();
            final Object attributesBuilder = CLASS_AUDIO_ATTRIBUTES_BUILDER.newInstance();
            final Object attributes;
            final Object formatBuilder = CLASS_AUDIO_FORMAT_BUILDER.newInstance();
            final Object format;
            final int channelConfig;
            int bufferSize;

            switch (channels) {
                case 1:
                    channelConfig = CHANNEL_OUT_MONO;
                    break;
                case 2:
                    channelConfig = CHANNEL_OUT_STEREO;
                    break;

                default:
                    throw new IllegalStateException("Unexpected number of channels: " + channels);
            }

            bufferSize = (int)invoke(CLASS_AUDIO_TRACK.getMethod("getMinBufferSize", Integer.TYPE, Integer.TYPE, Integer.TYPE),
                    audioTrack,
                    sampleRate, channelConfig, ENCODING_PCM_16BIT);
            bufferSize *= BUFFER_SCALE;

            invokeSetter("setUsage", attributesBuilder, getIntField(CLASS_AUDIO_ATTRIBUTES, "USAGE_MEDIA"));
            invokeSetter("setContentType", attributesBuilder, getIntField(CLASS_AUDIO_ATTRIBUTES, "CONTENT_TYPE_MUSIC"));
            attributes = invokeBuild(attributesBuilder);

            invokeSetter("setChannelMask", formatBuilder, CHANNEL_OUT_STEREO);
            invokeSetter("setEncoding", formatBuilder, ENCODING_PCM_16BIT);
            invokeSetter("setSampleRate", formatBuilder, sampleRate);
            format = invokeBuild(formatBuilder);

            audioTrack = CLASS_AUDIO_TRACK.getConstructor(attributes.getClass(), format.getClass(), Integer.TYPE, Integer.TYPE, Integer.TYPE).
                    newInstance(attributes, format, bufferSize, getIntField(CLASS_AUDIO_TRACK, "MODE_STREAM"), getIntField(CLASS_AUDIO_MANAGER, "AUDIO_SESSION_ID_GENERATE"));
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void deConfigureBackend() throws IOException {
        invoke(METHOD_AUDIO_TRACK_FLUSH, audioTrack);
        invoke(METHOD_AUDIO_TRACK_STOP, audioTrack);
        invoke(METHOD_AUDIO_TRACK_RELEASE, audioTrack);
    }

    @Override
    protected void writeToBackend(@NotNull PCMDataBlock block) throws IOException {
        final short[] buffer = block.getData();
        final int ret;

        ret = (int)invoke(METHOD_AUDIO_TRACK_WRITE, audioTrack, buffer, 0, buffer.length);
        if (ret != buffer.length) {
            throw new RuntimeException("Short write");
        }

        block.audible();
    }

    @Override
    protected boolean available() {
        return true;
    }

    @Override
    public synchronized void play() {
        super.play();
        invoke(METHOD_AUDIO_TRACK_PLAY, Objects.requireNonNull(audioTrack));
    }
}
