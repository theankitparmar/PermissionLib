package com.theankitparmar.permissionlib.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.theankitparmar.permissionlib.R

/**
 * Default dialog shown when a permission is permanently denied or a rationale is needed.
 *
 * Configuration ([DialogConfig]) is persisted via the Fragment's argument Bundle so the
 * dialog survives configuration changes (e.g. screen rotation). Click callbacks are
 * NOT serialisable — if the Fragment is recreated from saved state without callbacks
 * (e.g. process death) the dialog is dismissed automatically.
 */
class PermissionDialog : DialogFragment() {

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
            title              = getString(KEY_TITLE,         "Permission Required")!!,
            message            = getString(KEY_MESSAGE,       "This permission is required.")!!,
            positiveButtonText = getString(KEY_POSITIVE_TEXT, "Open Settings")!!,
            negativeButtonText = getString(KEY_NEGATIVE_TEXT, "Cancel")!!,
            positiveButtonColor = getInt(KEY_POSITIVE_COLOR,  Color.parseColor("#2196F3")),
            negativeButtonColor = getInt(KEY_NEGATIVE_COLOR,  Color.parseColor("#757575")),
            backgroundColor    = getInt(KEY_BG_COLOR,         Color.WHITE),
            titleFontSize      = getFloat(KEY_TITLE_SIZE,     18f),
            messageFontSize    = getFloat(KEY_MESSAGE_SIZE,   14f),
            titleTextColor     = getInt(KEY_TITLE_TEXT_COLOR, Color.parseColor("#212121")),
            messageTextColor   = getInt(KEY_MSG_TEXT_COLOR,   Color.parseColor("#616161")),
            isCancelable       = getBoolean(KEY_CANCELABLE,   true)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If recreated from saved state (e.g. config change or process death),
        // the lambda callbacks can't be restored — dismiss safely.
        if (savedInstanceState != null && onPositiveClick == null) {
            dismissAllowingStateLoss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_permission, container, false)
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
        applyConfig(view)
    }

    private fun applyConfig(view: View) {
        // Root card background
        val cardView = view.findViewById<View>(R.id.permission_dialog_root)
        cardView.background = GradientDrawable().apply {
            setColor(config.backgroundColor)
            cornerRadius = 16f.dpToPx(requireContext())
        }

        // Title
        view.findViewById<TextView>(R.id.tv_permission_title).apply {
            text = config.title
            setTextColor(config.titleTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, config.titleFontSize)
        }

        // Message
        view.findViewById<TextView>(R.id.tv_permission_message).apply {
            text = config.message
            setTextColor(config.messageTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, config.messageFontSize)
        }

        // Negative button (Cancel)
        view.findViewById<Button>(R.id.btn_permission_cancel).apply {
            text = config.negativeButtonText
            setTextColor(config.negativeButtonColor)
            setOnClickListener {
                onNegativeClick?.invoke()
                dismissAllowingStateLoss()
            }
        }

        // Positive button (Open Settings / Continue)
        view.findViewById<Button>(R.id.btn_permission_settings).apply {
            text = config.positiveButtonText
            setTextColor(config.positiveButtonColor)
            background = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
            setOnClickListener {
                onPositiveClick?.invoke()
                dismissAllowingStateLoss()
            }
        }
    }

    private fun Float.dpToPx(context: Context): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics)
}
