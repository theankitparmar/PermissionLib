package com.theankitparmar.permissionlib.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.theankitparmar.permissionlib.databinding.DialogPermissionBinding

/**
 * Default dialog shown when a permission is permanently denied or a rationale is needed.
 *
 * Configuration ([DialogConfig]) is persisted via the Fragment's argument Bundle so the
 * dialog survives configuration changes (e.g. screen rotation). Click callbacks are
 * NOT serialisable — if the Fragment is recreated from saved state without callbacks
 * (e.g. process death) the dialog is dismissed automatically.
 */
class PermissionDialog : DialogFragment() {

    private var _binding: DialogPermissionBinding? = null
    private val binding get() = _binding!!

    private var onPositiveClick: (() -> Unit)? = null
    private var onNegativeClick: (() -> Unit)? = null

    private val config: DialogConfig by lazy { requireArguments().toDialogConfig() }

    companion object {
        private const val KEY_TITLE            = "title"
        private const val KEY_MESSAGE          = "message"
        private const val KEY_POSITIVE_TEXT    = "positive_text"
        private const val KEY_NEGATIVE_TEXT    = "negative_text"
        private const val KEY_POSITIVE_COLOR   = "positive_color"
        private const val KEY_NEGATIVE_COLOR   = "negative_color"
        private const val KEY_BG_COLOR         = "bg_color"
        private const val KEY_TITLE_SIZE       = "title_size"
        private const val KEY_MESSAGE_SIZE     = "message_size"
        private const val KEY_TITLE_TEXT_COLOR = "title_text_color"
        private const val KEY_MSG_TEXT_COLOR   = "msg_text_color"
        private const val KEY_CANCELABLE       = "cancelable"

        /** Create a new [PermissionDialog] with the given configuration. */
        fun newInstance(
            config: DialogConfig,
            onPositive: () -> Unit,
            onNegative: (() -> Unit)? = null
        ): PermissionDialog {
            return PermissionDialog().apply {
                arguments = config.toBundle()
                this.onPositiveClick = onPositive
                this.onNegativeClick = onNegative
            }
        }

        private fun DialogConfig.toBundle() = Bundle().apply {
            putString(KEY_TITLE,            title)
            putString(KEY_MESSAGE,          message)
            putString(KEY_POSITIVE_TEXT,    positiveButtonText)
            putString(KEY_NEGATIVE_TEXT,    negativeButtonText)
            putInt(KEY_POSITIVE_COLOR,      positiveButtonColor)
            putInt(KEY_NEGATIVE_COLOR,      negativeButtonColor)
            putInt(KEY_BG_COLOR,            backgroundColor)
            putFloat(KEY_TITLE_SIZE,        titleFontSize)
            putFloat(KEY_MESSAGE_SIZE,      messageFontSize)
            putInt(KEY_TITLE_TEXT_COLOR,    titleTextColor)
            putInt(KEY_MSG_TEXT_COLOR,      messageTextColor)
            putBoolean(KEY_CANCELABLE,      isCancelable)
        }

        private fun Bundle.toDialogConfig() = DialogConfig(
            title               = getString(KEY_TITLE,         "Permission Required")!!,
            message             = getString(KEY_MESSAGE,       "This permission is required.")!!,
            positiveButtonText  = getString(KEY_POSITIVE_TEXT, "Open Settings")!!,
            negativeButtonText  = getString(KEY_NEGATIVE_TEXT, "Cancel")!!,
            positiveButtonColor = getInt(KEY_POSITIVE_COLOR,   DialogConfig.Defaults.COLOR_PRIMARY),
            negativeButtonColor = getInt(KEY_NEGATIVE_COLOR,   DialogConfig.Defaults.COLOR_GRAY),
            backgroundColor     = getInt(KEY_BG_COLOR,         Color.WHITE),
            titleFontSize       = getFloat(KEY_TITLE_SIZE,     18f),
            messageFontSize     = getFloat(KEY_MESSAGE_SIZE,   14f),
            titleTextColor      = getInt(KEY_TITLE_TEXT_COLOR, DialogConfig.Defaults.COLOR_TEXT_PRIMARY),
            messageTextColor    = getInt(KEY_MSG_TEXT_COLOR,   DialogConfig.Defaults.COLOR_TEXT_SECONDARY),
            isCancelable        = getBoolean(KEY_CANCELABLE,   true)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If recreated from saved state (e.g. config change or process death),
        // lambda callbacks can't be restored — dismiss safely rather than showing
        // a non-functional dialog.
        if (savedInstanceState != null && onPositiveClick == null) {
            dismissAllowingStateLoss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.requestFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(config.isCancelable)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyConfig()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyConfig() {
        // Root card background
        binding.permissionDialogRoot.background = GradientDrawable().apply {
            setColor(config.backgroundColor)
            cornerRadius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics
            )
        }

        // Title
        binding.tvPermissionTitle.apply {
            text = config.title
            setTextColor(config.titleTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, config.titleFontSize)
        }

        // Message
        binding.tvPermissionMessage.apply {
            text = config.message
            setTextColor(config.messageTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, config.messageFontSize)
        }

        // Negative button (Cancel)
        binding.btnPermissionCancel.apply {
            text = config.negativeButtonText
            setTextColor(config.negativeButtonColor)
            setOnClickListener {
                onNegativeClick?.invoke()
                dismissAllowingStateLoss()
            }
        }

        // Positive button (Open Settings / Continue)
        binding.btnPermissionSettings.apply {
            text = config.positiveButtonText
            setTextColor(config.positiveButtonColor)
            background = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
            setOnClickListener {
                onPositiveClick?.invoke()
                dismissAllowingStateLoss()
            }
        }
    }
}
