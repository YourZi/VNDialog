package top.yourzi.dialog.util;

import org.apache.commons.compress.utils.IOUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

public class STBBackendImage extends BufferedImage implements AutoCloseable {

    private final int width;
    private final int height;
    private long pointer;

    private STBBackendImage(int width, int height, long pointer) {
        super(width, height, TYPE_INT_ARGB);
        this.width = width;
        this.height = height;
        this.pointer = pointer;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {

        for (int z = startY; z < h; z++) {
            for (int x = startX; x < w; x++) {
                int color = MemoryUtil.memGetInt(this.pointer + ((x + (long) z * width) * 4));
                // ABGR -> ARGB
                int a = (color >> 24) & 0xFF;
                int b = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int r = (color >> 0) & 0xFF;

                int finalColor = (a << 24) | (r << 16) | (g << 8) | b;

                rgbArray[x + (z * width)] = finalColor;
            }
        }

        return rgbArray;
    }

    @Override
    public int getRGB(int x, int z) {
        checkBounds(x, z);

        return MemoryUtil.memGetInt(this.pointer + ((x + (long) z * width) * 4));
    }

    @Override
    public void setRGB(int x, int z, int rgb) {
        checkBounds(x, z);

        MemoryUtil.memPutInt(this.pointer + ((x + (long) z * width) * 4), rgb);
    }

    @Override
    public BufferedImage getSubimage(int x, int y, int w, int h) {
        throw new UnsupportedOperationException("不可能到达之地");
    }

    @Override
    public void close() {
        if (this.pointer != 0) {
            STBImage.nstbi_image_free(this.pointer);

            this.pointer = 0;
        }
    }

    private void checkBounds(int x, int z) {
        if (x < 0 || x >= this.width || z < 0 || z >= this.height) {
            throw new IllegalStateException(
                    "越界: " + x + ", " + z + " (width: " + this.width + ", height: " + this.height + ")");
        }
    }

    public static STBBackendImage read(final InputStream stream) throws IOException {
        ByteBuffer imgBuf = null;

        try {
            imgBuf = readResource(stream);
            imgBuf.rewind();
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                IntBuffer width = memoryStack.mallocInt(1);
                IntBuffer height = memoryStack.mallocInt(1);
                IntBuffer channels = memoryStack.mallocInt(1);

                // 4 channels: RGBA
                ByteBuffer buf = STBImage.stbi_load_from_memory(imgBuf, width, height, channels, 4);
                if (buf == null) {
                    throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
                }

                return new STBBackendImage(width.get(0), height.get(0), MemoryUtil.memAddress(buf));
            }

        } finally {
            //释放
            MemoryUtil.memFree(imgBuf);
            IOUtils.closeQuietly(stream);
        }
    }

    private static ByteBuffer readResource(InputStream inputStream) throws IOException {
        ByteBuffer byteBuffer;
        if (inputStream instanceof FileInputStream) {
            FileChannel fileChannel = ((FileInputStream) inputStream).getChannel();
            byteBuffer = MemoryUtil.memAlloc((int) fileChannel.size() + 1);

            while (fileChannel.read(byteBuffer) != -1) {}
        } else {
            int sizeGuess = 4096;
            try {
                sizeGuess = Math.max(4096, inputStream.available());
            } catch (IOException ignored) {}

            byteBuffer = MemoryUtil.memAlloc(sizeGuess * 2);
            ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);

            while (readableByteChannel.read(byteBuffer) != -1) {
                if (byteBuffer.remaining() == 0) {
                    byteBuffer = MemoryUtil.memRealloc(byteBuffer, byteBuffer.capacity() * 2);
                }
            }
        }

        return byteBuffer;
    }
}
