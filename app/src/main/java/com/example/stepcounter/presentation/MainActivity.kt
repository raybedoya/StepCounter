/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.stepcounter.presentation

import android.health.connect.datatypes.ExerciseCompletionGoal
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.example.stepcounter.R
import com.example.stepcounter.presentation.theme.StepCounterTheme

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.getSystemService

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var heartRate by mutableIntStateOf(72)

    private val heartRatePermissionLauncher = registerForActivityResult(
        ActivityResultContracts
            .RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            registerHeartRateSensor()
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        val heartRatePermission = getHeartRatePermission()
        if (ContextCompat.checkSelfPermission(
                this,
                heartRatePermission
            ) != PackageManager.PERMISSION_GRANTED
        )
            heartRatePermissionLauncher.launch(
                heartRatePermission
            )

        setContent {
            StepCounterTheme {
                WearFitnessApp(
                    heartRateSensorValue = heartRate,
                    hasHeartRateSensor = heartRateSensor != null
                )
            }
        }
    }

    private fun getHeartRatePermission():
            String {
        return if (Build.VERSION.SDK_INT >= 36) {
            "android.permission.health.READ_HEART_RATE"
        } else {
            Manifest.permission.BODY_SENSORS
        }
    }

    private fun registerHeartRateSensor() {
        val permission = getHeartRatePermission()
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        heartRateSensor?.let { sensor -> sensorManager.registerListener(this, sensor,
                SensorManager.SENSOR_DELAY_NORMAL)}
    }

    private fun createNotificationChannel(
        context: Context
    ) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Fitness Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Heart-rate and activity reminders"
        }
        val notificationManager = getSystemService(context, NotificationManager::class.java)

        notificationManager?.createNotificationChannel(
            channel
        )
    }

    override fun onResume() {
        super.onResume()
        registerHeartRateSensor()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(
        event: SensorEvent?
    ) {
        if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
            heartRate = event.values[0].toInt()
        }

    }

}

@Composable
fun WearFitnessApp(heartRateSensorValue: Int, hasHeartRateSensor: Boolean) {

    val navController = rememberNavController();
    val context = LocalContext.current

    var heartRateNotificationSent by remember {
        mutableStateOf(false)
    }

    var steps by remember { mutableIntStateOf(30) }

    var calories by remember { mutableIntStateOf(25) }

    var stepsGoal by remember { mutableIntStateOf(10000) }

    var caloriesGoal by remember { mutableIntStateOf(500) }

    var heartRate by remember { mutableIntStateOf(72) }

    var manualHeartRate by remember {
        mutableIntStateOf(72)
    }

    val displayedHeartRate =
        if(hasHeartRateSensor) {
            heartRateSensorValue
        } else{
            manualHeartRate
        }

    var notificationPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted = isGranted
    }



    LaunchedEffect(
        displayedHeartRate,
        notificationPermissionGranted
    ) {
        if (
            displayedHeartRate >= 100 &&
            !heartRateNotificationSent &&
            notificationPermissionGranted
        ) {
            showNotification(
                context = context,
                notificationId = HEART_RATE_NOTIFICATION_ID,
                title = "High Heart Rate Detected",
                message = "Your heart rate reached $displayedHeartRate BPM."
            )
            heartRateNotificationSent = true
        }
        if(displayedHeartRate < 100) {
            heartRateNotificationSent = false
        }
    }

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionGranted
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        if (heartRate < 100) {
            heartRateNotificationSent = false
        }

    }


    SwipeNavigationContainer(
        navController = navController
    ) {

        NavHost(
            navController = navController,
            startDestination = "progress"
        ) {
            composable(route = "progress") {
                DailyProgressScreen(
                    steps = steps,
                    calories = calories,
                    stepsGoal = stepsGoal,
                    caloriesGoal = caloriesGoal,
                    onAddStep = {
                        steps++
                        calories++
                    }
                )
            }
            composable(route = "heart") {
                HeartRateScreen(
                    heartRate = displayedHeartRate,
                    hasHeartRateSensor = hasHeartRateSensor,
                    onDecreaseHeartRate = {
                        manualHeartRate--
                    },
                    onIncreaseHeartRate = {
                        manualHeartRate++
                    }
                )
            }
            composable(route = "goals") {
                ModifyGoalScreen(
                    stepsGoal = stepsGoal,
                    caloriesGoal = caloriesGoal,
                    onDecreaseStepsGoal = { stepsGoal -= 500 },
                    onIncreaseStepsGoal = { stepsGoal += 500 },
                    onDecreaseCaloriesGoal = { caloriesGoal -= 50 },
                    onIncreaseCaloriesGoal = { caloriesGoal += 50 }
                )

            }
        }
    }
}

@Composable
fun SwipeNavigationContainer(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val routes = listOf(
        "progress",
        "heart",
        "goals"
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "progress"
    val currentIndex = routes.indexOf(currentRoute)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(currentRoute) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (
                            totalDrag < -60 &&
                            currentIndex < routes.lastIndex
                        ) {
                            navController.navigate(
                                routes[currentIndex + 1]
                            ) {
                                launchSingleTop = true
                            }
                        }
                        if (
                            totalDrag > 60 &&
                            currentIndex > 0
                        ) {
                            navController.navigate(
                                routes[currentIndex - 1]
                            ) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()

    }

}

@Composable
fun DailyProgressScreen(
    steps: Int,
    calories: Int,
    stepsGoal: Int,
    caloriesGoal: Int,
    onAddStep: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Text(
            text = "Daily Progress",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Steps",
            color = Color.White
        )

        Text(
            text = "$steps / $stepsGoal",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Calories", color = Color.White)

        Text(
            text = "$calories / $stepsGoal",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onAddStep) {
            Text("Add Step")
        }


    }

}

@Composable
fun HeartRateScreen(
    heartRate: Int,
    hasHeartRateSensor: Boolean,
    onDecreaseHeartRate: () -> Unit,
    onIncreaseHeartRate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Text(
            text = "Heart Rate",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "$heartRate BPM",
            color = Color.White,
            style = MaterialTheme.typography.displaySmall
        )
        if(!hasHeartRateSensor) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = onDecreaseHeartRate
                ) {
                    Text("-")
                }
                Spacer(modifier = Modifier.width(8.dp)
                )
                Button(
                    onClick = onIncreaseHeartRate
                ) {
                    Text("+")
                }
            }
        }

    }
}

@Composable
fun ModifyGoalScreen(
    stepsGoal: Int,
    caloriesGoal: Int,
    onDecreaseStepsGoal: () -> Unit,
    onIncreaseStepsGoal: () -> Unit,
    onDecreaseCaloriesGoal: () -> Unit,
    onIncreaseCaloriesGoal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Text(
            text = "Modify Goal",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Steps",
            color = Color.White
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = onDecreaseStepsGoal) {
                Text("_")
            }
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stepsGoal.toString(),
                color = Color.White

            )
            Spacer(modifier = Modifier.width(6.dp))

            Button(onClick = onIncreaseStepsGoal) {
                Text("+")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))


        Text(
            text = "Steps",
            color = Color.White
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = onDecreaseCaloriesGoal) {
                Text("-")
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = caloriesGoal.toString(),
                color = Color.White
            )

            Spacer(modifier = Modifier.width(6.dp))

            Button(onClick = onIncreaseCaloriesGoal) {
                Text("+")
            }
        }
    }

}

fun showNotification(
    context: Context,
    notificationId: Int,
    title: String,
    message: String
) {
    if (
        Build.VERSION.SDK_INT <
        Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED

    ) {
        return
    }
    val notification =
        NotificationCompat.Builder(
            context,
            CHANNEL_ID
        ).setSmallIcon(
            android.R.drawable.ic_dialog_info

        )
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(
                Notification.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .build()
    NotificationManagerCompat
        .from(context)
        .notify(
            notificationId,
            notification
        )

}





