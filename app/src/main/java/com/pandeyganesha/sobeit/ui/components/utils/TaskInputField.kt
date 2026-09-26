package com.pandeyganesha.kaamsutra.ui.components.utils

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

private val InitialHeight = 56.dp
private val ExpandedDefaultHeight = 120.dp
private val MaxHeight = 220.dp
private val FieldPadding = 12.dp

@Composable
fun TaskInputField(
    modifier: Modifier = Modifier,
    text: String,
    onTextChange: (String) -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val hasBody = text.contains('\n')
    val title = text.substringBefore('\n')
    val body = if (hasBody) text.substringAfter('\n') else ""

    val bodyFocusRequester = remember { FocusRequester() }
    var requestBodyFocus by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (hasBody) bodyFocusRequester.requestFocus() else focusRequester?.requestFocus()
        keyboard?.show()
    }

    LaunchedEffect(requestBodyFocus) {
        if (requestBodyFocus) {
            bodyFocusRequester.requestFocus()
            requestBodyFocus = false
        }
    }
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val borderColor = MaterialTheme.colorScheme.outline
    val scrollState = rememberScrollState()

    LaunchedEffect(text) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(4.dp))
                .heightIn(
                    min = if (hasBody) ExpandedDefaultHeight else InitialHeight,
                    max = MaxHeight,
                )
                .verticalScroll(scrollState)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TitleField(
                    value = title,
                    onValueChange = { newTitle ->
                        val sanitized = newTitle.replace("\n", "") // title never holds a real newline
                        onTextChange(if (hasBody) "$sanitized\n$body" else sanitized)
                    },
                    onEnter = {
                        if (!hasBody) onTextChange("$title\n$body") // establishes the body, empty for now
                        requestBodyFocus = true
                    },
                    focusRequester = focusRequester,
                    placeholderColor = placeholderColor,
                )

                if (hasBody) {
                    HorizontalDivider(
                        color = dividerColor,
                        modifier = Modifier.padding(horizontal = FieldPadding, vertical = 6.dp)
                    )
                    BodyField(
                        value = body,
                        onValueChange = { newBody -> onTextChange("$title\n$newBody") },
                        focusRequester = bodyFocusRequester,
                        placeholderColor = placeholderColor,
                    )
                }
            }
        }

        if (!hasBody && title.isNotEmpty()) {
            Text(
                text = "Press enter to add description",
                style = MaterialTheme.typography.bodySmall,
                color = placeholderColor,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
private fun TitleField(
    value: String,
    onValueChange: (String) -> Unit,
    onEnter: () -> Unit,
    focusRequester: FocusRequester?,
    placeholderColor: Color,
) {
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    BasicTextField(
        value = fieldValue,
        onValueChange = { new ->
            fieldValue = new
            onValueChange(new.text)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(FieldPadding)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = LocalContentColor.current),
        cursorBrush = SolidColor(LocalContentColor.current),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onEnter() }),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(text = "Title", style = MaterialTheme.typography.titleMedium, color = placeholderColor)
            }
            innerTextField()
        }
    )
}

@Composable
private fun BodyField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    placeholderColor: Color,
) {
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    BasicTextField(
        value = fieldValue,
        onValueChange = { new ->
            fieldValue = new
            onValueChange(new.text)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FieldPadding, vertical = 4.dp)
            .focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = LocalContentColor.current),
        cursorBrush = SolidColor(LocalContentColor.current),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(text = "Description (optional)", style = MaterialTheme.typography.bodyMedium, color = placeholderColor)
            }
            innerTextField()
        }
    )
}