package org.cardboardpowered.mixin.block;

import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShapeContext.class)
public interface MixinShapeContext {

	/**
	 * Make ShapeContext.of(null) return absent instead of throwing an NPE for WorldImpl#rayTraceBlocks
	 */
	@Inject(at = @At("HEAD"), method = "of", cancellable = true)
	private static void of(Entity entity, CallbackInfoReturnable<ShapeContext> cir) {
		if (entity == null) {
			cir.setReturnValue(ShapeContext.absent());
		}
	}

}
