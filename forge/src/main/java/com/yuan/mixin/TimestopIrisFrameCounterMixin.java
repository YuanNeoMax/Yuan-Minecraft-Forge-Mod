package com.yuan.mixin;

import com.yuan.timestop.YuanTimeStop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.SystemTimeUniforms$FrameCounter", remap = false)
public abstract class TimestopIrisFrameCounterMixin {
    @Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void yuanTimestopIrisFrameCounter(CallbackInfo ci) {
        if (YuanTimeStop.get()) {
            ci.cancel();
        }
    }
}
