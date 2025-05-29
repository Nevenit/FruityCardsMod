package com.fruitycards

import com.fruitycards.WeeklyCardDetection.scheduleChestRead
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.ItemEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.Field

object FruityCards : ModInitializer {
	private const val MOD_ID = "fruitycards"
	val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

	var weekly_cards = mutableSetOf<String>()

	private val minecraftColors = mapOf(
		"black" to Triple(0, 0, 0),
		"dark_blue" to Triple(0, 0, 170),
		"dark_green" to Triple(0, 170, 0),
		"dark_aqua" to Triple(0, 170, 170),
		"dark_red" to Triple(170, 0, 0),
		"dark_purple" to Triple(170, 0, 170),
		"gold" to Triple(255, 170, 0),
		"gray" to Triple(200, 200, 200),
		"dark_gray" to Triple(85, 85, 85),
		"blue" to Triple(85, 85, 255),
		"green" to Triple(85, 255, 85),
		"aqua" to Triple(85, 255, 255),
		"red" to Triple(255, 85, 85),
		"light_purple" to Triple(255, 85, 255),
		"yellow" to Triple(255, 255, 85),
		"white" to Triple(255, 255, 255)
	)

	// Cached reflection fields
	var xField: Field? = null
	var yField: Field? = null

	private var weeklyListOpen = false
	var checkWeekly = false

	init {
		try {
			xField = HandledScreen::class.java.getDeclaredField("x")
			yField = HandledScreen::class.java.getDeclaredField("y")
			xField?.isAccessible = true
			yField?.isAccessible = true
		} catch (e: Exception) {
			LOGGER.warn("Could not access HandledScreen fields via reflection: ${e.message}")
		}
	}

	override fun onInitialize() {
		// Listen for all incoming chat messages
		ClientReceiveMessageEvents.GAME.register { message, _ ->
			// Detect when we join the skyblock server
			if (message.string == "● FRUITSKYBLOCK") {
				checkWeekly = true
				val client = MinecraftClient.getInstance()
				if (!weeklyListOpen) {
					WeeklyCardDetection.openChest(client)
					weeklyListOpen = true
				}
			}
		}

		ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
			if (screen is HandledScreen<*>) {
				ScreenEvents.afterRender(screen).register { _, drawContext, _, _, _ ->
					UIRendering.highlightSlotsInScreen(screen, drawContext)

					if (checkWeekly && screen is GenericContainerScreen) {
						// Add a small delay and retry mechanism
						scheduleChestRead(screen)
					}
				}
			}
		}

		WorldRenderEvents.AFTER_ENTITIES.register { context ->
			val world = context.world()

			world?.entities?.forEach { entity ->
				if (entity is ItemEntity && isItemACard(entity.stack)) {
					val colour = getHighlightColour(entity.stack);
					if (world.isClient) {
						WorldRendering.spawnParticles(entity, world, rgbToHex(colour.first, colour.second, colour.third))
					}
				}
			}
		}

		LOGGER.info("Paper Highlighter Client initialized!")
	}

	fun isItemACard(itemStack: ItemStack): Boolean {
		if (itemStack.item != Items.PAPER) return false
		if (itemStack.customName == null) return false
		if (!itemStack.customName!!.toString().startsWith("empty[siblings=[literal{[}[style={color=yellow,bold,!italic,!underlined,!strikethrough,!obfuscated}], literal{CARD}[style={color=gold,bold,!italic}], literal{] }[style={color=yellow,bold,!italic}], literal")) return false
		return true
	}

	fun isItemAWeeklyCard(itemStack: ItemStack): Boolean {
		if (itemStack.item != Items.PAPER) return false
		if (itemStack.customName == null) return false
		if (!itemStack.customName!!.toString().startsWith("empty[siblings=[literal{[}[style={color=yellow,bold,!italic,!underlined,!strikethrough,!obfuscated}], literal{CARD}[style={color=gold,bold,!italic}], literal{] }[style={color=yellow,bold,!italic}], literal")) return false

		val displayName = itemStack.name.string
		return weekly_cards.any { cardName -> fuzzyMatch(cardName, displayName, 1) }
	}

	fun getHighlightColour(itemStack: ItemStack): Triple<Float, Float, Float>  {
		val (name, colourName) = extractNameAndColor(itemStack.customName?.toString() ?: "") ?: return Triple(1f, 1f, 1f) // default to white
		val (r255, g255, b255) = minecraftColors[colourName.lowercase()] ?: Triple(255, 255, 255)

		val r = r255 / 255f
		val g = g255 / 255f
		val b = b255 / 255f

		return Triple(r, g, b)
	}

	private fun extractNameAndColor(input: String): Pair<String, String>? {
		val regex = Regex("""literal\{([^}]+)}\[style=\{color=([^,}]+)""")
		return regex.findAll(input)
			.lastOrNull() // get the last literal-style block, assumed to be the dynamic one
			?.let { match ->
				val name = match.groupValues[1]
				val color = match.groupValues[2]
				name to color
			}
	}

	fun rgbToHex(r: Float, g: Float, b: Float, alpha: Float = 1f): Int {
		val a = (alpha * 255).toInt().coerceIn(0, 255)
		val red = (r * 255).toInt().coerceIn(0, 255)
		val green = (g * 255).toInt().coerceIn(0, 255)
		val blue = (b * 255).toInt().coerceIn(0, 255)

		return (a shl 24) or (red shl 16) or (green shl 8) or blue
	}

	fun levenshteinDistance(s1: String, s2: String): Int {
		val len1 = s1.length
		val len2 = s2.length

		// Create a matrix to store distances
		val matrix = Array(len1 + 1) { IntArray(len2 + 1) }

		// Initialize first row and column
		for (i in 0..len1) {
			matrix[i][0] = i
		}
		for (j in 0..len2) {
			matrix[0][j] = j
		}

		// Fill the matrix
		for (i in 1..len1) {
			for (j in 1..len2) {
				val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
				matrix[i][j] = minOf(
					matrix[i - 1][j] + 1,      // deletion
					matrix[i][j - 1] + 1,      // insertion
					matrix[i - 1][j - 1] + cost // substitution
				)
			}
		}

		return matrix[len1][len2]
	}

	fun fuzzyMatch(str1: String, str2: String, threshold: Int = 1): Boolean {
		return levenshteinDistance(str1.lowercase(), str2.lowercase()) <= threshold
	}

}