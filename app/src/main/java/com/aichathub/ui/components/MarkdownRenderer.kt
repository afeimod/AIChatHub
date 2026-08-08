package com.aichathub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 轻量级 Markdown 渲染器 — 支持：
 * - 标题 H1~H6
 * - 粗体 / 斜体 / 删除线 / 行内代码
 * - 代码块（```）
 * - 无序列表 (-, *, +)
 * - 有序列表 (1., 2.)
 * - 引用 (>)
 * - 分割线 (---)
 * - 链接 [text](url)
 * - 段落
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 15
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    Column(modifier = modifier) {
        blocks.forEach { block -> renderBlock(block, fontSize) }
    }
}

private sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Code(val language: String, val content: String) : MarkdownBlock()
    data class ListItem(val ordered: Boolean, val index: Int, val content: String) : MarkdownBlock()
    data class Quote(val text: String) : MarkdownBlock()
    data class Paragraph(val content: String) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

private fun parseMarkdown(src: String): List<MarkdownBlock> {
    val lines = src.replace("\r\n", "\n").split("\n")
    val out = mutableListOf<MarkdownBlock>()
    var i = 0
    val codeBuf = StringBuilder()
    var codeLang = ""
    var inCode = false

    while (i < lines.size) {
        val line = lines[i]
        if (line.startsWith("```")) {
            if (inCode) {
                out.add(MarkdownBlock.Code(codeLang, codeBuf.toString().trimEnd()))
                codeBuf.clear()
                codeLang = ""
                inCode = false
            } else {
                codeLang = line.removePrefix("```").trim()
                inCode = true
            }
            i++; continue
        }
        if (inCode) {
            codeBuf.append(line).append('\n')
            i++; continue
        }
        if (line.isBlank()) { i++; continue }
        if (line.startsWith("---") || line.startsWith("***")) {
            out.add(MarkdownBlock.Divider); i++; continue
        }
        // 标题
        val headingMatch = Regex("^(#{1,6})\\s+(.*)").find(line)
        if (headingMatch != null) {
            out.add(MarkdownBlock.Heading(headingMatch.groupValues[1].length, headingMatch.groupValues[2]))
            i++; continue
        }
        // 引用
        if (line.startsWith("> ")) {
            out.add(MarkdownBlock.Quote(line.removePrefix("> ")))
            i++; continue
        }
        // 有序列表
        val orderedMatch = Regex("^(\\d+)\\.\\s+(.*)").find(line)
        if (orderedMatch != null) {
            out.add(MarkdownBlock.ListItem(true, orderedMatch.groupValues[1].toInt(), orderedMatch.groupValues[2]))
            i++; continue
        }
        // 无序列表
        if (line.matches(Regex("^[\\-*\\+]\\s+.*"))) {
            out.add(MarkdownBlock.ListItem(false, 0, line.substring(2).trim()))
            i++; continue
        }
        // 普通段落
        out.add(MarkdownBlock.Paragraph(line.trim()))
        i++
    }
    if (inCode && codeBuf.isNotEmpty()) {
        out.add(MarkdownBlock.Code(codeLang, codeBuf.toString().trimEnd()))
    }
    return out
}

@Composable
private fun renderBlock(block: MarkdownBlock, fontSize: Int) {
    when (block) {
        is MarkdownBlock.Heading -> {
            val size = when (block.level) {
                1 -> fontSize + 10
                2 -> fontSize + 8
                3 -> fontSize + 6
                4 -> fontSize + 4
                else -> fontSize + 2
            }
            Text(
                text = parseInline(block.text),
                fontSize = size.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }
        is MarkdownBlock.Code -> CodeBlock(block.content, block.language, fontSize)
        is MarkdownBlock.ListItem -> {
            Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
                Text(
                    text = if (block.ordered) "${block.index}. " else "• ",
                    fontSize = fontSize.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = parseInline(block.content),
                    fontSize = fontSize.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        is MarkdownBlock.Quote -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
            ) {
                Text(
                    text = parseInline(block.text),
                    fontSize = fontSize.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    fontStyle = FontStyle.Italic
                )
            }
        }
        is MarkdownBlock.Divider -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )
        }
        is MarkdownBlock.Paragraph -> {
            Text(
                text = parseInline(block.content),
                fontSize = fontSize.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun CodeBlock(content: String, language: String, fontSize: Int) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            if (language.isNotBlank()) {
                Text(
                    text = language,
                    fontSize = 11.sp,
                    color = Color(0xFF888888),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                text = content,
                fontSize = (fontSize - 1).sp,
                color = Color(0xFFD4D4D4),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            )
        }
    }
}

/** 解析行内样式：粗体、斜体、行内代码、链接、删除线 */
private fun parseInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        // 行内代码 `code`
        if (text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > i) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFFE0E0E0), color = Color(0xFFC7254E))) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1; continue
            }
        }
        // 粗体 **text**
        if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
            val end = text.indexOf("**", i + 2)
            if (end > i) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(i + 2, end))
                }
                i = end + 2; continue
            }
        }
        // 斜体 *text*
        if (text[i] == '*') {
            val end = text.indexOf('*', i + 1)
            if (end > i && end != i + 1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1; continue
            }
        }
        // 删除线 ~~text~~
        if (i + 1 < text.length && text[i] == '~' && text[i + 1] == '~') {
            val end = text.indexOf("~~", i + 2)
            if (end > i) {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(text.substring(i + 2, end))
                }
                i = end + 2; continue
            }
        }
        // 链接 [text](url)
        if (text[i] == '[') {
            val close = text.indexOf(']', i + 1)
            if (close > i && close + 1 < text.length && text[close + 1] == '(') {
                val endParen = text.indexOf(')', close + 2)
                if (endParen > close) {
                    val label = text.substring(i + 1, close)
                    val url = text.substring(close + 2, endParen)
                    pushStringAnnotation(tag = "URL", annotation = url)
                    withStyle(SpanStyle(color = Color(0xFF1565C0), textDecoration = TextDecoration.Underline)) {
                        append(label)
                    }
                    pop()
                    i = endParen + 1; continue
                }
            }
        }
        append(text[i]); i++
    }
}
