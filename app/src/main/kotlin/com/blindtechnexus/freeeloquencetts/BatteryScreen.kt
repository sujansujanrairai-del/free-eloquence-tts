package com.blindtechnexus.freeeloquencetts

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(onBack: () -> Unit, onRequestExemption: () -> Unit) {
	val context = LocalContext.current
	val settings = remember { Settings(context.applicationContext) }

	val noBatteryScreen = stringResource(R.string.battery_no_screen)
	val bootNeedsNotification = stringResource(R.string.battery_boot_needs_notification)
	val notificationOn = stringResource(R.string.battery_notification_on)
	val notificationOff = stringResource(R.string.battery_notification_off)

	var granted by remember { mutableStateOf(BatteryExemption.isGranted(context)) }
	var persistent by remember { mutableStateOf(settings.persistentNotification) }
	var startOnBoot by remember { mutableStateOf(settings.startOnBoot) }
	var notice by remember {
		mutableStateOf(if (settings.persistentNotification) notificationOn else notificationOff)
	}
	var stopButton by remember { mutableStateOf(settings.notificationStopButton) }
	var tracking by remember { mutableStateOf(settings.trackActions) }

	val activity = context as? ComponentActivity
	DisposableEffect(settings) {
		onDispose { settings.close() }
	}
	DisposableEffect(activity) {
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_RESUME) {
				granted = BatteryExemption.isGranted(context)
				persistent = settings.persistentNotification
			}
		}
		activity?.lifecycle?.addObserver(observer)
		onDispose { activity?.lifecycle?.removeObserver(observer) }
	}

	Scaffold(
		topBar = {
			ScreenTopBar(title = stringResource(R.string.section_battery), onBack = onBack)
		}
	) { inset ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(inset)
				.verticalScroll(rememberScrollState())
				.padding(bottom = 24.dp)
		) {
			SectionText(text = stringResource(R.string.battery_screen_description))
			LiveNotice(text = notice)

			SectionHeading(text = stringResource(R.string.battery_idle_heading))
			ExemptionRow(
				granted = granted,
				onClick = {
					Tracker.action(settings, "asked for the battery optimization exemption")
					onRequestExemption()
				}
			)
			SwitchRow(
				label = stringResource(R.string.persistent_notification_label),
				checked = persistent,
				description = stringResource(R.string.persistent_notification_description),
				onChange = { on ->
					persistent = on
					settings.persistentNotification = on

					KeepAlive.sync(context, on)

					notice = if (on) notificationOn else notificationOff
					Tracker.action(settings, "persistent notification ${if (on) "on" else "off"}")
				}
			)
			SwitchRow(
				label = stringResource(R.string.notification_stop_label),
				checked = stopButton,
				description = stringResource(R.string.notification_stop_description),
				onChange = { on ->
					stopButton = on
					settings.notificationStopButton = on
					if (persistent) KeepAlive.sync(context, true)
					Tracker.action(settings, "notification stop button ${if (on) "on" else "off"}")
				}
			)
			SwitchRow(
				label = stringResource(R.string.start_on_boot_label),
				checked = startOnBoot,
				description = stringResource(R.string.start_on_boot_description),
				onChange = { on ->
					startOnBoot = on
					settings.startOnBoot = on
					if (on && !persistent) {

						persistent = true
						settings.persistentNotification = true
						KeepAlive.sync(context, true)
						notice = bootNeedsNotification
					}
					Tracker.action(settings, "start on boot ${if (on) "on" else "off"}")
				}
			)
			ActionRow(
				label = stringResource(
					if (ManufacturerScreens.hasOwnScreen()) R.string.battery_manufacturer_open
					else R.string.battery_android_open
				),
				description = stringResource(R.string.battery_manufacturer_description),
				onClick = {
					Tracker.action(settings, "asked for the phone's own background settings")
					if (!ManufacturerScreens.open(context) && !ManufacturerScreens.appDetails(context)) {
						notice = noBatteryScreen
					}
				}
			)

			SectionHeading(text = stringResource(R.string.battery_fault_heading))
			SwitchRow(
				label = stringResource(R.string.track_actions_label),
				checked = tracking,
				description = stringResource(R.string.track_actions_description),
				onChange = { on ->
					tracking = on
					settings.trackActions = on
					Tracker.action(settings, "action tracking ${if (on) "on" else "off"}")
				}
			)
			Paragraph(
				text = stringResource(
					R.string.battery_now,
					if (persistent) stringResource(R.string.battery_now_wanted)
					else stringResource(R.string.battery_now_unwanted),
					if (KeepAliveService.isRunning) stringResource(R.string.battery_now_service)
					else stringResource(R.string.battery_now_none),
					if (KeepAliveNotification.notificationsAllowed(context))
						stringResource(R.string.battery_now_allowed)
					else stringResource(R.string.battery_now_refused)
				)
			)
		}
	}
}

@Composable
private fun ExemptionRow(granted: Boolean, onClick: () -> Unit) {
	val state = stringResource(
		if (granted) R.string.battery_exemption_granted else R.string.battery_exemption_not_granted
	)
	val label = stringResource(R.string.battery_exemption_label)
	Described(description = stringResource(R.string.battery_exemption_description)) {
		OutlinedButton(
			onClick = onClick,
			modifier = Modifier
				.fillMaxWidth()
				.clearAndSetSemantics {
					contentDescription = label
					stateDescription = state
					role = Role.Button
				}
		) {
			Text(text = label)
			Text(text = " $state")
		}
	}
}
