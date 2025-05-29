package com.fruitycards

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.ItemStack

object WeeklyCardDetection {
    private var chestReadAttempts = 0
    private var lastReadTime = 0L
    private const val MAX_READ_ATTEMPTS = 5
    private const val READ_DELAY_MS = 50L

    fun scheduleChestRead(chestScreen: GenericContainerScreen) {
        val currentTime = System.currentTimeMillis()

        // Only attempt to read every 100ms to avoid spam
        if (currentTime - lastReadTime < READ_DELAY_MS) {
            return
        }
        lastReadTime = currentTime

        // Try reading the chest
        if (readChestItems(chestScreen)) {
            // Success - reset attempts and stop checking
            chestReadAttempts = 0
            FruityCards.checkWeekly = false
            closeChest()
        } else {
            chestReadAttempts++
            if (chestReadAttempts >= MAX_READ_ATTEMPTS) {
                // Give up after too many attempts
                FruityCards.LOGGER.warn("Failed to read chest contents after $MAX_READ_ATTEMPTS attempts")
                chestReadAttempts = 0
                FruityCards.checkWeekly = false
                closeChest()
            }
        }
    }

    fun openChest(client: MinecraftClient) {
        // Send the command to open weekly card list
        client.networkHandler?.sendChatCommand("listcards")
    }

    fun readChestItems(chestScreen: GenericContainerScreen): Boolean {
        val handler = chestScreen.screenHandler
        val inventory = handler.inventory
        val items = mutableListOf<ItemStack>()

        // Read all items from the chest (excluding player inventory slots)
        val chestSlots = handler.rows * 9 // Chest slots only
        var hasItems = false

        for (i in 0 until chestSlots) {
            val stack = inventory.getStack(i)
            if (!stack.isEmpty) {
                items.add(stack.copy())
                hasItems = true
            }
        }

        // If we found no items and this is one of our first attempts,
        // the chest might still be loading
        if (!hasItems && chestReadAttempts < 3) {
            return false
        }


        items.forEachIndexed { index, stack ->
            val customName = stack.customName?.string ?: ""

            if (customName.isNotEmpty()) {
                if (customName.contains("CARD", ignoreCase = true)) {
                    FruityCards.weekly_cards.add(customName)
                }
            }
        }

        return true // Successfully read the chest (even if empty)
    }

    private fun closeChest() {
        val client = MinecraftClient.getInstance()
        client.player?.closeHandledScreen()
    }
}