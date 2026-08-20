package com.example.futurediary.ui.util

/**
 * A Domain Specific Language (DSL) for building diary entry templates.
 */
class DiaryTemplate(val name: String, val content: String)

class TemplateBuilder {
    private val sb = StringBuilder()

    fun section(title: String) {
        sb.append("\n**$title**\n")
    }

    fun prompt(text: String) {
        sb.append("_ ${text} _\n")
    }

    fun bullets(count: Int) {
        for (i in 1..count) {
            sb.append("$i. \n")
        }
    }

    fun line() {
        sb.append("\n---\n")
    }

    fun build(): String = sb.toString().trim()
}

/**
 * The entry point for our DSL. 
 * It takes a lambda with [TemplateBuilder] as the receiver.
 */
fun diaryTemplate(name: String, block: TemplateBuilder.() -> Unit): DiaryTemplate {
    val builder = TemplateBuilder()
    builder.block()
    return DiaryTemplate(name, builder.build())
}

object TemplateRegistry {
    val templates = listOf(
        diaryTemplate("Gratitude Journal") {
            section("Today I am grateful for:")
            bullets(3)
            line()
            prompt("What was the highlight of your day?")
        },
        diaryTemplate("Travel Log") {
            section("Location & Vibe")
            prompt("Where are you today?")
            line()
            section("Memories")
            prompt("What's one thing you never want to forget about this trip?")
            bullets(2)
        },
        diaryTemplate("Deep Reflection") {
            section("Inner Thoughts")
            prompt("What's on your mind lately?")
            line()
            section("Lessons Learned")
            prompt("What did today teach you?")
        }
    )
}
