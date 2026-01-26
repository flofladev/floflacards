package com.floflacards.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.floflacards.app.R
import com.floflacards.app.data.entity.FlashcardEntity
import com.floflacards.app.domain.model.FlashcardRating
import com.floflacards.app.domain.usecase.SrsUseCase
import com.floflacards.app.domain.usecase.SimpleStreakUseCase
import com.floflacards.app.data.repository.FlashcardRepository
import com.floflacards.app.data.dao.CategoryDao
import com.floflacards.app.data.repository.SettingsRepository
import com.floflacards.app.data.source.FlashcardUiPreferences
import com.floflacards.app.service.ViewModelStoreManager
import com.floflacards.app.service.LearningServiceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    
    companion object {
        private const val TAG = "OverlayService"
        private const val EXTRA_FLASHCARD_ID = "flashcard_id"
        private const val EXTRA_FLASHCARD_QUESTION = "flashcard_question"
        private const val EXTRA_FLASHCARD_ANSWER = "flashcard_answer"
        private const val EXTRA_FLASHCARD_CATEGORY_ID = "flashcard_category_id"
        private const val EXTRA_FLASHCARD_EASINESS_FACTOR = "flashcard_easiness_factor"
        private const val EXTRA_FLASHCARD_REVIEW_COUNT = "flashcard_review_count"
        private const val EXTRA_FLASHCARD_IS_ENABLED = "flashcard_is_enabled"
        private const val EXTRA_FLASHCARD_CORRECT_COUNT = "flashcard_correct_count"
        private const val EXTRA_FLASHCARD_INCORRECT_COUNT = "flashcard_incorrect_count"
        private const val EXTRA_FLASHCARD_LAST_REVIEWED_AT = "flashcard_last_reviewed_at"
        private const val EXTRA_FLASHCARD_COOLDOWN_UNTIL = "flashcard_cooldown_until"
        private const val EXTRA_FLASHCARD_CREATED_AT = "flashcard_created_at"
        private const val EXTRA_FLASHCARD_UPDATED_AT = "flashcard_updated_at"
        private const val EXTRA_FLASHCARD_QUESTION_IMAGE_PATH = "flashcard_question_image_path"
        private const val EXTRA_FLASHCARD_ANSWER_IMAGE_PATH = "flashcard_answer_image_path"
        
        fun startWithFlashcard(context: Context, flashcard: FlashcardEntity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                Log.e(TAG, "Overlay permission not granted")
                return
            }
            
            val intent = Intent(context, OverlayService::class.java).apply {
                putExtra(EXTRA_FLASHCARD_ID, flashcard.id)
                putExtra(EXTRA_FLASHCARD_QUESTION, flashcard.question)
                putExtra(EXTRA_FLASHCARD_ANSWER, flashcard.answer)
                putExtra(EXTRA_FLASHCARD_CATEGORY_ID, flashcard.categoryId)
                putExtra(EXTRA_FLASHCARD_EASINESS_FACTOR, flashcard.easinessFactor)
                putExtra(EXTRA_FLASHCARD_REVIEW_COUNT, flashcard.reviewCount)
                putExtra(EXTRA_FLASHCARD_IS_ENABLED, flashcard.isEnabled)
                putExtra(EXTRA_FLASHCARD_CORRECT_COUNT, flashcard.correctCount)
                putExtra(EXTRA_FLASHCARD_INCORRECT_COUNT, flashcard.incorrectCount)
                putExtra(EXTRA_FLASHCARD_LAST_REVIEWED_AT, flashcard.lastReviewedAt)
                putExtra(EXTRA_FLASHCARD_COOLDOWN_UNTIL, flashcard.cooldownUntil)
                putExtra(EXTRA_FLASHCARD_CREATED_AT, flashcard.createdAt)
                putExtra(EXTRA_FLASHCARD_UPDATED_AT, flashcard.updatedAt)
                putExtra(EXTRA_FLASHCARD_QUESTION_IMAGE_PATH, flashcard.questionImagePath)
                putExtra(EXTRA_FLASHCARD_ANSWER_IMAGE_PATH, flashcard.answerImagePath)
            }
            
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start overlay service", e)
            }
        }
        
        /**
         * Starts overlay service with demo flashcard for first-time users.
         * Follows SRP by separating demo logic from regular flashcard display.
         */
        fun startWithDemoFlashcard(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                Log.e(TAG, "Overlay permission not granted for demo")
                return
            }
            
            // Create demo flashcard with educational content
            val demoFlashcard = FlashcardEntity(
                id = -1L, // Special ID to indicate demo
                categoryId = -1L,
                question = context.getString(R.string.demo_welcome_question),
                answer = context.getString(R.string.demo_welcome_answer),
                isEnabled = true,
                easinessFactor = 2.5f,
                reviewCount = 0,
                correctCount = 0,
                incorrectCount = 0,
                lastReviewedAt = 0L,
                cooldownUntil = 0L,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            startWithFlashcard(context, demoFlashcard)
        }
    }
    
    @Inject
    lateinit var srsUseCase: SrsUseCase
    
    @Inject
    lateinit var simpleStreakUseCase: SimpleStreakUseCase
    
    @Inject
    lateinit var flashcardRepository: FlashcardRepository
    
    @Inject
    lateinit var categoryDao: CategoryDao
    
    @Inject
    lateinit var settingsManager: SettingsRepository
    
    @Inject
    lateinit var flashcardUiPreferences: FlashcardUiPreferences
    
    @Inject
    lateinit var viewModelStoreManager: ViewModelStoreManager
    
    @Inject
    lateinit var learningServiceManager: LearningServiceManager
    
    // Extracted components following SOLID principles
    private lateinit var overlayManager: OverlayManager
    private lateinit var overlayComponents: OverlayComponents
    
    private var flashcard: FlashcardEntity? = null
    
    // Service scope for coroutines
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Lifecycle components
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore
        get() = viewModelStoreManager.getOverlayViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OverlayService onCreate")
        try {
            // Initialize extracted components following SOLID principles
            overlayManager = OverlayManager(this, flashcardUiPreferences)
            overlayComponents = OverlayComponents(categoryDao, flashcardUiPreferences, settingsManager)
            
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            stopSelf()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "OverlayService onStartCommand")
        try {
            val flashcard = intent?.let { extractFlashcardFromIntent(it) }
            if (flashcard != null) {
                this.flashcard = flashcard
                
                // Mark demo as running if this is a demo flashcard
                if (flashcard.id == -1L) {
                    settingsManager.setDemoRunning(true)
                    Log.d(TAG, "Demo flashcard started, marked as running")
                }
                
                showOverlay(flashcard)
            } else {
                Log.e(TAG, "No flashcard data in intent")
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand", e)
            stopSelf()
        }
        return START_NOT_STICKY
    }
    
    private fun extractFlashcardFromIntent(intent: Intent): FlashcardEntity? {
        return FlashcardEntity(
            id = intent.getLongExtra(EXTRA_FLASHCARD_ID, 0),
            categoryId = intent.getLongExtra(EXTRA_FLASHCARD_CATEGORY_ID, 0),
            question = intent.getStringExtra(EXTRA_FLASHCARD_QUESTION) ?: "",
            answer = intent.getStringExtra(EXTRA_FLASHCARD_ANSWER) ?: "",
            questionImagePath = intent.getStringExtra(EXTRA_FLASHCARD_QUESTION_IMAGE_PATH),
            answerImagePath = intent.getStringExtra(EXTRA_FLASHCARD_ANSWER_IMAGE_PATH),
            isEnabled = intent.getBooleanExtra(EXTRA_FLASHCARD_IS_ENABLED, true),
            easinessFactor = intent.getFloatExtra(EXTRA_FLASHCARD_EASINESS_FACTOR, 2.5f),
            reviewCount = intent.getIntExtra(EXTRA_FLASHCARD_REVIEW_COUNT, 0),
            correctCount = intent.getIntExtra(EXTRA_FLASHCARD_CORRECT_COUNT, 0),
            incorrectCount = intent.getIntExtra(EXTRA_FLASHCARD_INCORRECT_COUNT, 0),
            lastReviewedAt = intent.getLongExtra(EXTRA_FLASHCARD_LAST_REVIEWED_AT, 0),
            cooldownUntil = intent.getLongExtra(EXTRA_FLASHCARD_COOLDOWN_UNTIL, 0),
            createdAt = intent.getLongExtra(EXTRA_FLASHCARD_CREATED_AT, System.currentTimeMillis()),
            updatedAt = intent.getLongExtra(EXTRA_FLASHCARD_UPDATED_AT, System.currentTimeMillis())
        )
    }
    
    private fun showOverlay(flashcard: FlashcardEntity) {
        val success = overlayManager.showOverlay(
            lifecycleOwner = this,
            viewModelStoreOwner = this,
            savedStateRegistryOwner = this
        ) {
            overlayComponents.OverlayContent(
                flashcard = flashcard,
                onPositionChange = { deltaX, deltaY ->
                    overlayManager.updateWindowPositionRelative(deltaX, deltaY)
                },
                onSizeChange = { deltaWidth, deltaHeight ->
                    overlayManager.updateWindowSizeRelative(deltaWidth, deltaHeight)
                },
                onRating = { rating -> handleFlashcardRating(flashcard, rating) },
                onClose = { handleFlashcardClose(flashcard) },
                onManageCards = { handleManageCardsNavigation() }
            )
        }
        
        if (success) {
            // STREAK UPDATE: Track flashcard view activity for streak calculation
            // This is called when flashcard becomes visible to user (both regular and demo)
            try {
                val updatedStreak = simpleStreakUseCase.recordFlashcardActivity()
                Log.d(TAG, "Streak updated: current=${updatedStreak.currentStreak}, highest=${updatedStreak.highestStreak}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update streak: ${e.message}") // Non-critical, don't fail overlay
            }
        } else {
            Log.e(TAG, "Failed to show overlay")
            stopSelf()
        }
    }
    

    
    private fun handleFlashcardRating(flashcard: FlashcardEntity, rating: FlashcardRating) {
        Log.d(TAG, "Handling flashcard rating: $rating")
        
        // Check if this is a demo flashcard (ID = -1)
        if (flashcard.id == -1L) {
            Log.d(TAG, "Demo flashcard completed, not updating SRS data")
            handleDemoCompletion()
        } else {
            // Regular flashcard - update SRS data
            serviceScope.launch(Dispatchers.IO) {
                try {
                    // Fetch the latest flashcard data from database to avoid stale data
                    val latestFlashcard = flashcardRepository.getFlashcardById(flashcard.id)
                    if (latestFlashcard != null) {
                        srsUseCase.updateFlashcardRating(latestFlashcard, rating)
                    } else {
                        Log.w(TAG, "Could not find flashcard with id=${flashcard.id}, using original")
                        srsUseCase.updateFlashcardRating(flashcard, rating)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating flashcard rating", e)
                }
            }
            
            // Close overlay and resume timer for regular flashcards
            closeOverlay()
            resumeTimerAfterInteraction()
        }
        
        // Auto-reset to normal mode after rating using extracted components
        overlayComponents.resetToNormalMode()
    }
    
    private fun handleFlashcardClose(flashcard: FlashcardEntity) {
        Log.d(TAG, "Handling flashcard close (skip)")
        
        // Check if this is a demo flashcard
        if (flashcard.id == -1L) {
            Log.d(TAG, "Demo flashcard closed, not resuming timer")
            handleDemoCompletion()
        } else {
            // Regular flashcard - resume timer
            closeOverlay()
            resumeTimerAfterInteraction()
        }
    }
    
    /**
     * Handles manage button click from empty state overlay.
     * Stops the learning service and opens the app on home screen.
     */
    private fun handleManageCardsNavigation() {
        Log.d(TAG, "Stopping learning service and opening home screen")
        
        // Stop the learning service first
        learningServiceManager.stopLearningService()
        
        // Close overlay
        closeOverlay()
        
        // Launch the app using package manager's launch intent
        try {
            val packageManager = packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(launchIntent)
                Log.d(TAG, "Successfully launched app")
            } else {
                Log.w(TAG, "Could not get launch intent for package")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app", e)
        }
    }
    
    /**
     * Handles completion of demo flashcard.
     * Follows SRP by separating demo completion logic.
     * CRITICAL FIX: Use LearningServiceManager to properly start learning with all state updates.
     */
    private fun handleDemoCompletion() {
        Log.d(TAG, "Demo completed, starting real learning session")
        
        // Close demo overlay
        closeOverlay()
        
        // Mark demo as no longer running and as completed
        settingsManager.setDemoRunning(false)
        settingsManager.setFirstDemoShown()
        
        // Start the real learning session using LearningServiceManager
        // This ensures proper state management (isLearningActive, UI state, etc.)
        serviceScope.launch {
            try {
                learningServiceManager.startLearningService(settingsManager.getIntervalMinutes())
                Log.d(TAG, "Learning service started after demo completion")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting real learning session after demo", e)
            }
        }
    }
    
    private fun closeOverlay() {
        Log.d(TAG, "Closing overlay")
        
        // Transition lifecycle to prevent new animations
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        
        // Use extracted OverlayManager for cleanup following SOLID principles
        overlayManager.closeOverlay(serviceScope) {
            // Stop the service after overlay cleanup is complete
            stopSelf()
        }
    }
    
    private fun resumeTimerAfterInteraction() {
        try {
            // CRITICAL FIX: Check if learning is still active before resuming timer
            if (!settingsManager.getIsLearningActive()) {
                Log.d(TAG, "Learning is stopped, not resuming timer after interaction")
                return
            }
            
            Log.d(TAG, "Resuming timer after interaction")
            // Start a new timer cycle after user interaction
            val intent = Intent(this, TimerForegroundService::class.java).apply {
                putExtra("interval_minutes", settingsManager.getIntervalMinutes())
            }
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming timer", e)
        }
    }
    
    override fun onDestroy() {
        Log.d(TAG, "OverlayService onDestroy")
        try {
            // Transition lifecycle to destroyed state
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            
            // Cancel all coroutines
            serviceScope.cancel()
            
            // Clean up demo state if this was a demo flashcard
            flashcard?.let { fc ->
                if (fc.id == -1L) {
                    settingsManager.setDemoRunning(false)
                    Log.d(TAG, "Demo state cleared during service destruction")
                }
            }
            
            // Release ViewModelStore reference to prevent memory leaks
            viewModelStoreManager.releaseOverlayViewModelStore()
            
            // Use extracted OverlayManager for emergency cleanup
            overlayManager.forceCleanup()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        } finally {
            super.onDestroy()
        }
    }
}
