package me.impy.aegis.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// LittleByteBuffer wraps a ByteBuffer to extend its API a little.
// Its byte order is set to little endian by default.
// All this boilerplate just to change the default byte order and add a peek method... Is it worth it? Probably not.
public class LittleByteBuffer {
    private ByteBuffer _buffer;

    private LittleByteBuffer(ByteBuffer buffer) {
        _buffer = buffer;
        _buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public byte peek() {
        _buffer.mark();
        byte b = _buffer.get();
        _buffer.reset();
        return b;
    }

    public byte get() { return _buffer.get(); }
    public LittleByteBuffer get(byte[] dst) {_buffer.get(dst); return this; }
    public LittleByteBuffer put(byte b) { _buffer.put(b); return this; }
    public LittleByteBuffer put(byte[] bytes) { _buffer.put(bytes); return this; }
    public int remaining() { return _buffer.remaining(); }
    public byte[] array() { return _buffer.array(); }
    public LittleByteBuffer putInt(int i) { _buffer.putInt(i); return this; }
    public LittleByteBuffer putLong(long l) { _buffer.putLong(l); return this; }
    public int getInt() { return _buffer.getInt(); }
    public long getLong() { return _buffer.getLong(); }
    public int position() { return _buffer.position(); }
    public LittleByteBuffer position(int i) { _buffer.position(i); return this; }
    public static LittleByteBuffer allocate(int size) { return new LittleByteBuffer(ByteBuffer.allocate(size)); }
    public static LittleByteBuffer wrap(byte[] bytes) { return new LittleByteBuffer(ByteBuffer.wrap(bytes)); }
}
