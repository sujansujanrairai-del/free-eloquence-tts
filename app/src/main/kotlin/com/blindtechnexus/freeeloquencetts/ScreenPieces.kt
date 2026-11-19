package com.blindtechnexus.freeeloquencetts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(title: String, onBack: (() -> Unit)? = null) {
	CenterAlignedTopAppBar(
		title = {
			Text(
				text = title,
				style = MaterialTheme.typography.titleLarge,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.semantics { heading() }
			)
		},
		navigationIcon = {

			val back = onBack
			if (back != null) BackButton(onBack = back)
		}
	)
}

@Composable
fun BackButton(onBack: () -> Unit) {
	val label = stringResource(R.string.go_back)
	Text(
		text = label,
		style = MaterialTheme.typography.labelLarge,
		color = MaterialTheme.colorScheme.primary,
		modifier = Modifier

			.clickable(onClick = onBack)
			.padding(horizontal = 16.dp, vertical = 14.dp)
			.clearAndSetSemantics {
				contentDescription = label
				role = Role.Button
			}
	)
}

@Composable
fun SectionHeading(text: String, horizontal: Dp = 16.dp) {
	Text(
		text = text,
		style = MaterialTheme.typography.titleMedium,
		modifier = Modifier
			.fillMaxWidth()
			.padding(start = horizontal, end = horizontal, top = 24.dp, bottom = 4.dp)
			.semantics { heading() }
	)
}

@Composable
fun SectionText(text: String) {
	Text(
		text = text,
		style = MaterialTheme.typography.bodyMedium,
		modifier = Modifier
			.fillMaxWidth()
			.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
	)
}

@Composable
fun Paragraph(text: String) {
	Text(
		text = text,
		style = MaterialTheme.typography.bodyLarge,
		modifier = Modifier
			.fillMaxWidth()
			.padding(bottom = 16.dp)
	)
}

@Composable
fun LiveNotice(text: String, horizontal: Dp = 0.dp) {
	Text(
		text = text,
		style = MaterialTheme.typography.bodyMedium,
		color = if (text.isEmpty()) MaterialTheme.colorScheme.onSurface
		else MaterialTheme.colorScheme.error,
		modifier = Modifier
			.fillMaxWidth()
			.padding(start = horizontal, end = horizontal, bottom = 8.dp)
			.semantics { liveRegion = LiveRegionMode.Polite }
	)
}

@Composable
fun Gap(height: Dp = 16.dp) {
	Spacer(modifier = Modifier.height(height))
}
