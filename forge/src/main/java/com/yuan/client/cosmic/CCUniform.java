package com.yuan.client.cosmic;

import com.mojang.blaze3d.shaders.Shader;
import com.mojang.blaze3d.shaders.Uniform;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.util.Arrays;

public abstract class CCUniform extends Uniform implements ICCUniform {
    protected final UniformType type;

    protected CCUniform(String name, UniformType type, int count, @Nullable Shader parent) {
        super(name, type.getVanillaType(), count, parent);
        this.type = type;
        if (this.intValues != null) {
            MemoryUtil.memFree(this.intValues);
            this.intValues = null;
        }
        if (this.floatValues != null) {
            MemoryUtil.memFree(this.floatValues);
            this.floatValues = null;
        }
    }

    static CCUniform makeUniform(String name, UniformType type, int count, @Nullable Shader parent) {
        if (count % type.getSize() != 0) {
            throw new IllegalArgumentException("Expected count to be a multiple of the uniform type size: " + type.getSize());
        }
        return switch (type.getCarrier()) {
            case INT, U_INT -> new IntUniform(name, type, count, parent);
            case FLOAT, MATRIX -> new FloatUniform(name, type, count, parent);
            case DOUBLE, D_MATRIX -> new DoubleUniform(name, type, count, parent);
        };
    }

    private static final class IntUniform extends UniformEntry<int[]> {
        public IntUniform(String name, UniformType type, int count, @Nullable Shader parent) {
            super(name, type, count, parent);
            assert type.getCarrier() == UniformType.Carrier.INT || type.getCarrier() == UniformType.Carrier.U_INT;
        }

        @Override
        public void glUniformI(int... values) {
            if (this.type.getCarrier() != UniformType.Carrier.INT && this.type.getCarrier() != UniformType.Carrier.U_INT) {
                throw new IllegalArgumentException("Uniform '%s' isn't registered with the carrier of INT or U_INT, Got type '%s' with carrier '%s'.".formatted(getName(), this.type, this.type.getCarrier()));
            }
            if (len(values) != getCount()) {
                throw new IllegalArgumentException("Invalid size for uniform '%s', Expected: '%s', Got: '%s'.".formatted(getName(), getCount(), len(values)));
            }
            if (!equals(this.cache, values) || this.transpose) {
                this.cache = values.clone();
                this.transpose = false;
                this.dirty = true;
            }
        }

        @Override
        public void glUniformF(boolean transpose, float... values) {
            throw new UnsupportedOperationException("IntUniform cannot set float values.");
        }

        @Override
        public void glUniformD(boolean transpose, double... values) {
            throw new UnsupportedOperationException("IntUniform cannot set double values.");
        }

        @Override
        public void flush() {
            assert this.cache != null;
            switch (this.type) {
                case INT -> GL20.glUniform1iv(this.getLocation(), this.cache);
                case U_INT -> GL30.glUniform1uiv(this.getLocation(), this.cache);
                case I_VEC2, B_VEC2 -> GL20.glUniform2iv(this.getLocation(), this.cache);
                case U_VEC2 -> GL30.glUniform2uiv(this.getLocation(), this.cache);
                case I_VEC3, B_VEC3 -> GL20.glUniform3iv(this.getLocation(), this.cache);
                case U_VEC3 -> GL30.glUniform3uiv(this.getLocation(), this.cache);
                case I_VEC4, B_VEC4 -> GL20.glUniform4iv(this.getLocation(), this.cache);
                case U_VEC4 -> GL30.glUniform4uiv(this.getLocation(), this.cache);
                default -> throw new IllegalStateException("Unhandled uniform type for IntUniform: " + this.type);
            }
        }

        @Override
        public int len(int[] cache) {
            return cache.length;
        }

        @Override
        public boolean equals(@Nullable int[] a, int[] b) {
            return Arrays.equals(a, b);
        }
    }

    private static final class FloatUniform extends UniformEntry<float[]> {
        public FloatUniform(String name, UniformType type, int count, @Nullable Shader parent) {
            super(name, type, count, parent);
            assert type.getCarrier() == UniformType.Carrier.FLOAT || type.getCarrier() == UniformType.Carrier.MATRIX;
        }

        @Override
        public void glUniformF(boolean transpose, float... values) {
            if (this.type.getCarrier() != UniformType.Carrier.FLOAT && this.type.getCarrier() != UniformType.Carrier.MATRIX) {
                throw new IllegalArgumentException("Uniform '%s' isn't registered with the carrier of FLOAT or MATRIX, Got type '%s' with carrier '%s'.".formatted(getName(), this.type, this.type.getCarrier()));
            }
            if (len(values) != getCount()) {
                throw new IllegalArgumentException("Invalid size for uniform '%s', Expected: '%s', Got: '%s'.".formatted(getName(), getCount(), len(values)));
            }
            if (!equals(this.cache, values) || this.transpose != transpose) {
                this.cache = values.clone();
                this.transpose = transpose;
                this.dirty = true;
            }
        }

        @Override
        public void glUniformI(int... values) {
            throw new UnsupportedOperationException("FloatUniform cannot set int values.");
        }

        @Override
        public void glUniformD(boolean transpose, double... values) {
            throw new UnsupportedOperationException("FloatUniform cannot set double values.");
        }

        @Override
        public void flush() {
            assert this.cache != null;
            switch (this.type) {
                case FLOAT -> GL20.glUniform1fv(this.getLocation(), this.cache);
                case VEC2 -> GL20.glUniform2fv(this.getLocation(), this.cache);
                case VEC3 -> GL20.glUniform3fv(this.getLocation(), this.cache);
                case VEC4 -> GL20.glUniform4fv(this.getLocation(), this.cache);
                case MAT2 -> GL20.glUniformMatrix2fv(this.getLocation(), this.transpose, this.cache);
                case MAT2x3 -> GL21.glUniformMatrix2x3fv(this.getLocation(), this.transpose, this.cache);
                case MAT2x4 -> GL21.glUniformMatrix2x4fv(this.getLocation(), this.transpose, this.cache);
                case MAT3 -> GL20.glUniformMatrix3fv(this.getLocation(), this.transpose, this.cache);
                case MAT3x2 -> GL21.glUniformMatrix3x2fv(this.getLocation(), this.transpose, this.cache);
                case MAT3x4 -> GL21.glUniformMatrix3x4fv(this.getLocation(), this.transpose, this.cache);
                case MAT4 -> GL20.glUniformMatrix4fv(this.getLocation(), this.transpose, this.cache);
                case MAT4x2 -> GL21.glUniformMatrix4x2fv(this.getLocation(), this.transpose, this.cache);
                case MAT4x3 -> GL21.glUniformMatrix4x3fv(this.getLocation(), this.transpose, this.cache);
                default -> throw new IllegalStateException("Unhandled uniform type for FloatUniform: " + this.type);
            }
        }

        @Override
        public int len(float[] cache) {
            return cache.length;
        }

        @Override
        public boolean equals(@Nullable float[] a, float[] b) {
            return Arrays.equals(a, b);
        }
    }

    private static final class DoubleUniform extends UniformEntry<double[]> {
        public DoubleUniform(String name, UniformType type, int count, @Nullable Shader parent) {
            super(name, type, count, parent);
            assert type.getCarrier() == UniformType.Carrier.DOUBLE || type.getCarrier() == UniformType.Carrier.D_MATRIX;
        }

        @Override
        public void glUniformD(boolean transpose, double... values) {
            if (this.type.getCarrier() != UniformType.Carrier.DOUBLE && this.type.getCarrier() != UniformType.Carrier.D_MATRIX) {
                throw new IllegalArgumentException("Uniform '%s' isn't registered with the carrier of DOUBLE or D_MATRIX, Got type '%s' with carrier '%s'.".formatted(getName(), this.type, this.type.getCarrier()));
            }
            if (len(values) != getCount()) {
                throw new IllegalArgumentException("Invalid size for uniform '%s', Expected: '%s', Got: '%s'.".formatted(getName(), getCount(), len(values)));
            }
            if (!equals(this.cache, values) || this.transpose != transpose) {
                this.cache = values.clone();
                this.transpose = transpose;
                this.dirty = true;
            }
        }

        @Override
        public void glUniformI(int... values) {
            throw new UnsupportedOperationException("DoubleUniform cannot set int values.");
        }

        @Override
        public void glUniformF(boolean transpose, float... values) {
            throw new UnsupportedOperationException("DoubleUniform cannot set float values.");
        }

        @Override
        public void flush() {
            assert this.cache != null;
            switch (this.type) {
                case DOUBLE -> GL40.glUniform1dv(this.getLocation(), this.cache);
                case D_VEC2 -> GL40.glUniform2dv(this.getLocation(), this.cache);
                case D_VEC3 -> GL40.glUniform3dv(this.getLocation(), this.cache);
                case D_VEC4 -> GL40.glUniform4dv(this.getLocation(), this.cache);
                case D_MAT2 -> GL40.glUniformMatrix2dv(this.getLocation(), this.transpose, this.cache);
                case D_MAT2x3 -> GL40.glUniformMatrix2x3dv(this.getLocation(), this.transpose, this.cache);
                case D_MAT2x4 -> GL40.glUniformMatrix2x4dv(this.getLocation(), this.transpose, this.cache);
                case D_MAT3 -> GL40.glUniformMatrix3dv(this.getLocation(), this.transpose, this.cache);
                case D_MAT3x2 -> GL40.glUniformMatrix3x2dv(this.getLocation(), this.transpose, this.cache);
                case D_MAT3x4 -> GL40.glUniformMatrix3x4dv(this.getLocation(), this.transpose, this.cache);
                case D_MAT4 -> GL40.glUniformMatrix4dv(this.getLocation(), this.transpose, this.cache);
                case D_MAT4x2 -> GL40.glUniformMatrix4x2dv(this.getLocation(), this.transpose, this.cache);
                case D_MAT4x3 -> GL40.glUniformMatrix4x3dv(this.getLocation(), this.transpose, this.cache);
                default -> throw new IllegalStateException("Unhandled uniform type for DoubleUniform: " + this.type);
            }
        }

        @Override
        public int len(double[] cache) {
            return cache.length;
        }

        @Override
        public boolean equals(@Nullable double[] a, double[] b) {
            return Arrays.equals(a, b);
        }
    }

    private abstract static class UniformEntry<T> extends CCUniform {
        @Nullable
        protected T cache;
        protected boolean transpose;

        public UniformEntry(String name, UniformType type, int count, @Nullable Shader parent) {
            super(name, type, count, parent);
        }

        @Override
        public void set(float f0) {
            glUniformF(false, f0);
        }

        @Override
        public void set(float f0, float f1) {
            glUniformF(false, f0, f1);
        }

        @Override
        public void set(int i, float f) {
            throw new UnsupportedOperationException("Unable to set specific index.");
        }

        @Override
        public void set(float f0, float f1, float f2) {
            glUniformF(false, f0, f1, f2);
        }

        @Override
        public void set(Vector3f vec) {
            glUniformF(false, vec.x(), vec.y(), vec.z());
        }

        @Override
        public void set(float f0, float f1, float f2, float f3) {
            glUniformF(false, f0, f1, f2, f3);
        }

        @Override
        public void set(Vector4f vec) {
            glUniformF(false, vec.x(), vec.y(), vec.z(), vec.w());
        }

        @Override
        public void setSafe(float f0, float f1, float f2, float f3) {
            assert this.type.getCarrier() == UniformType.Carrier.FLOAT;
            switch (this.type.getSize()) {
                case 1 -> glUniform1f(f0);
                case 2 -> glUniform2f(f0, f1);
                case 3 -> glUniform3f(f0, f1, f2);
                case 4 -> glUniform4f(f0, f1, f2, f3);
                default -> throw new IllegalStateException("Unexpected type size: " + this.type);
            }
        }

        @Override
        public void setSafe(int i0, int i1, int i2, int i3) {
            assert this.type.getCarrier() == UniformType.Carrier.INT || this.type.getCarrier() == UniformType.Carrier.U_INT;
            switch (this.type.getSize()) {
                case 1 -> glUniform1i(i0);
                case 2 -> glUniform2i(i0, i1);
                case 3 -> glUniform3i(i0, i1, i2);
                case 4 -> glUniform4i(i0, i1, i2, i3);
                default -> throw new IllegalStateException("Unexpected type size: " + this.type);
            }
        }

        @Override
        public void set(int i0) {
            glUniformI(i0);
        }

        @Override
        public void set(int i0, int i1) {
            glUniformI(i0, i1);
        }

        @Override
        public void set(int i0, int i1, int i2) {
            glUniformI(i0, i1, i2);
        }

        @Override
        public void set(int i0, int i1, int i2, int i3) {
            glUniformI(i0, i1, i2, i3);
        }

        @Override
        public void set(float[] values) {
            glUniformF(false, values);
        }

        @Override
        public void setMat2x2(float m00, float m01, float m10, float m11) {
            glUniformF(true, m00, m01, m10, m11);
        }

        @Override
        public void setMat2x3(float m00, float m01, float m02, float m10, float m11, float m12) {
            glUniformF(true, m00, m01, m02, m10, m11, m12);
        }

        @Override
        public void setMat2x4(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13) {
            glUniformF(true, m00, m01, m02, m03, m10, m11, m12, m13);
        }

        @Override
        public void setMat3x2(float m00, float m01, float m10, float m11, float m20, float m21) {
            glUniformF(true, m00, m01, m10, m11, m20, m21);
        }

        @Override
        public void setMat3x3(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {
            glUniformF(true, m00, m01, m02, m10, m11, m12, m20, m21, m22);
        }

        @Override
        public void setMat3x4(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23) {
            glUniformF(true, m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23);
        }

        @Override
        public void setMat4x2(float m00, float m01, float m10, float m11, float m20, float m21, float m30, float m31) {
            glUniformF(true, m00, m01, m10, m11, m20, m21, m30, m31);
        }

        @Override
        public void setMat4x3(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22, float m30, float m31, float m32) {
            glUniformF(true, m00, m01, m02, m10, m11, m12, m20, m21, m22, m30, m31, m32);
        }

        @Override
        public void setMat4x4(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23, float m30, float m31, float m32, float m33) {
            glUniformF(true, m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33);
        }

        @Override
        public void set(Matrix4f mat) {
            glUniformMatrix4f(mat);
        }

        @Override
        public void set(Matrix3f mat) {
            glUniformMatrix3f(mat);
        }

        @Override
        public void upload() {
            if (!this.dirty) {
                return;
            }
            flush();
            this.dirty = false;
        }

        public abstract void flush();

        public abstract int len(T values);

        public abstract boolean equals(@Nullable T a, T b);
    }
}
