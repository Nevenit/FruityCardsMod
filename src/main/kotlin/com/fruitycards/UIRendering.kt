package com.fruitycards

import com.fruitycards.FruityCards.getHighlightColour
import com.fruitycards.FruityCards.isItemACard
import com.fruitycards.FruityCards.isItemAWeeklyCard
import com.fruitycards.FruityCards.rgbToHex
import com.fruitycards.FruityCards.xField
import com.fruitycards.FruityCards.yField
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import kotlin.math.sin

object UIRendering {
    fun highlightSlotsInScreen(screen: HandledScreen<*>, drawContext: DrawContext) {
        val screenHandler = screen.screenHandler

        var screenX = 0
        var screenY = 0

        try {
            // Try to get the actual screen position first
            if (xField != null && yField != null) {
                screenX = xField!!.getInt(screen)
                screenY = yField!!.getInt(screen)
            } else {
                val backgroundWidth = 176
                val backgroundHeight = when {
                    screen.javaClass.simpleName.contains("Chest") -> getChestHeight(screenHandler.slots.size)
                    screen.javaClass.simpleName.contains("Shulker") -> 166
                    else -> 166
                }

                screenX = (screen.width - backgroundWidth) / 2
                screenY = (screen.height - backgroundHeight) / 2
            }
        } catch (e: Exception) {
            screenX = (screen.width - 176) / 2
            screenY = (screen.height - 166) / 2
        }

        val time = System.currentTimeMillis()

        for (slot in screenHandler.slots) {
            val itemStack = slot.stack
            if (isItemACard(itemStack)) {
                val x = screenX + slot.x
                val y = screenY + slot.y

                val isWeeklyCard = isItemAWeeklyCard(itemStack)
                val colour = getHighlightColour(itemStack)

                if (!isWeeklyCard) {
                    drawRegularCardHighlight(drawContext, x, y, colour)
                }
            }
        }

        for (slot in screenHandler.slots) {
            val itemStack = slot.stack
            if (isItemACard(itemStack)) {
                val x = screenX + slot.x
                val y = screenY + slot.y

                val isWeeklyCard = isItemAWeeklyCard(itemStack)
                val colour = getHighlightColour(itemStack)

                if (isWeeklyCard) {
                    drawWeeklyCardHighlight(drawContext, x, y, colour, time)
                }
            }
        }
    }

    private fun getChestHeight(slotCount: Int): Int {
        return when {
            slotCount <= 27 -> 166 // Single chest
            slotCount <= 54 -> 222 // Double chest (166 + 56)
            else -> 166
        }
    }

    private fun drawWeeklyCardHighlight(
        drawContext: DrawContext,
        x: Int, y: Int,
        colour: Triple<Float, Float, Float>,
        time: Long
    ) {
        drawRegularCardHighlight(drawContext, x, y, colour)

        val pulse = (sin((time * 0.008)) * 0.3 + 0.7).toFloat()

        val borderColour = Triple(1.0f, 1.0f, 0.0f)

        // Animated double border
        val borderColor0 = rgbToHex(
            (colour.first + 0.3f).coerceAtMost(1.0f),
            (colour.second + 0.3f).coerceAtMost(1.0f),
            (colour.third + 0.3f).coerceAtMost(1.0f),
            1.0f * pulse
        )
        val borderColor1 = rgbToHex(borderColour.first, borderColour.second, borderColour.third, 1.0f)
        val borderColor2 = rgbToHex(
            (borderColour.first + 0.3f).coerceAtMost(1.0f),
            (borderColour.second + 0.3f).coerceAtMost(1.0f),
            (borderColour.third + 0.3f).coerceAtMost(1.0f),
            0.8f * pulse
        )

        drawContext.drawBorder(x , y, 16, 16, borderColor0)
        drawContext.drawBorder(x - 1, y - 1, 18, 18, borderColor1)
        drawContext.drawBorder(x - 2, y - 2, 20, 20, borderColor2)
    }

    private fun drawRegularCardHighlight(
        drawContext: DrawContext,
        x: Int, y: Int,
        colour: Triple<Float, Float, Float>,
    ) {
        // Colored fill
        val fillAlpha = 0.3f
        val fillColor = rgbToHex(colour.first, colour.second, colour.third, fillAlpha)
        drawContext.fill(x, y, x + 16, y + 16, fillColor)

        // Enhanced border for visibility
        val borderAlpha = 1f
        val borderColor = rgbToHex(colour.first, colour.second, colour.third, borderAlpha)
        drawContext.drawBorder(x - 1, y - 1, 18, 18, borderColor)
    }
}