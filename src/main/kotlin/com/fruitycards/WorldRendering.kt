package com.fruitycards

import net.minecraft.entity.ItemEntity
import net.minecraft.particle.DustParticleEffect
import net.minecraft.world.World
import kotlin.math.cos
import kotlin.math.sin

object WorldRendering {
    fun spawnParticles(entity: ItemEntity, world: World, colour: Int) {
        if (world.time % 4 != 0L) return

        val dustParticle = DustParticleEffect(colour, 1.0f)
        world.addParticle(
            dustParticle,
            entity.x + (world.random.nextDouble() - 0.5) * 0.5,
            entity.y + world.random.nextDouble() * 0.5,
            entity.z + (world.random.nextDouble() - 0.5) * 0.5,
            0.0, 0.0, 0.0
        )


        // Generate particles in a cone shape
        val height = world.random.nextDouble() * 1.5 // Height from 0 to 1.5
        val coneRadius = (1.5 - height) * 0.33 // Radius decreases as height increases


        // Generate random angle for circular distribution
        val angle = world.random.nextDouble() * 2 * Math.PI


        // Generate random distance from center (0 to coneRadius)
        val distance = world.random.nextDouble() * coneRadius


        // Calculate X and Z offsets using polar coordinates
        val xOffset = cos(angle) * distance
        val zOffset = sin(angle) * distance

        world.addParticle(
            dustParticle,
            entity.x + xOffset,
            entity.y + height,
            entity.z + zOffset,
            0.0, 0.0, 0.0
        )
    }
}