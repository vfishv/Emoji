package com.vanniktech.emoji

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.Px
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.vanniktech.emoji.emoji.Emoji
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

internal fun interface EmojiSearchDialogDelegate {
  fun onSearchEmojiClick(emoji: Emoji)
}

internal class EmojiSearchDialog : DialogFragment() {
  private var delegate: EmojiSearchDialogDelegate? = null
  private var searchEmoji: SearchEmoji? = null

  private val handler = Handler(Looper.getMainLooper())

  private var future: ScheduledFuture<*>? = null
  private val executorService = Executors.newSingleThreadScheduledExecutor()

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val activity = requireActivity()
    val dialog = AlertDialog.Builder(activity, theme)
      .setView(R.layout.emoji_dialog_search)
      .show()

    val root = dialog.findViewById<View>(R.id.root)
    val arguments = requireArguments()
    val theming = arguments.getParcelable<EmojiTheming>(ARG_THEMING)!!
    root?.setBackgroundColor(theming.backgroundColor(activity))

    val editText = dialog.findViewById<EditText>(R.id.editText)!!
    editText.setTextColor(theming.textColor(activity))
    val secondaryColor = theming.secondaryColor(activity)
    editText.setCursorDrawableColor(secondaryColor)
    editText.setHandlesColor(secondaryColor)
    editText.highlightColor = secondaryColor
    ViewCompat.setBackgroundTintList(editText, ColorStateList.valueOf(secondaryColor))

    val recyclerView = dialog.findViewById<RecyclerView>(R.id.recyclerView)
    val adapter = EmojiAdapter(
      theming = theming,
      emojiSearchDialogDelegate = delegate,
    )
    recyclerView?.adapter = adapter

    editText.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
      override fun afterTextChanged(s: Editable) {
        val query = s.toString()

        future?.cancel(true)
        future = executorService.schedule({
          val emojis = searchEmoji?.search(query).orEmpty()
          handler.post {
            adapter.update(emojis, marginStart = null)
          }
        }, 300, TimeUnit.MILLISECONDS)
      }
    })

    editText.postDelayed({
      editText.requestFocus()
      (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(editText, 0)
    }, 300L)

    return dialog
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    future?.cancel(true)
    executorService?.shutdownNow()
    handler.removeCallbacksAndMessages(null)
    delegate = null
  }

  internal companion object {
    private const val TAG = "EmojiSearchDialog"
    private const val ARG_THEMING = "arg-theming"

    @JvmStatic fun show(
      context: Context,
      delegate: EmojiSearchDialogDelegate,
      searchEmoji: SearchEmoji,
      theming: EmojiTheming,
    ) {
      EmojiSearchDialog().apply {
        arguments = Bundle(1).apply {
          putParcelable(ARG_THEMING, theming)
        }
        this.delegate = delegate
        this.searchEmoji = searchEmoji
        show((Utils.asActivity(context) as FragmentActivity).supportFragmentManager, TAG)
      }
    }
  }
}

internal class EmojiAdapter(
  private val theming: EmojiTheming,
  private val emojiSearchDialogDelegate: EmojiSearchDialogDelegate?,
) : RecyclerView.Adapter<EmojiViewHolder>() {
  @Px private var marginStart: Int? = null
  private var items = emptyList<SearchEmojiResult>()

  init {
    setHasStableIds(true)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = EmojiViewHolder(parent)

  override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
    val context = holder.textView.context
    val item = items[position]
    holder.textView.text = item.emoji.unicode

    (holder.textView.layoutParams as LinearLayout.LayoutParams).marginStart = marginStart ?: context.resources.getDimensionPixelSize(R.dimen.emoji_search_spacing)

    val shortCode = item.shortcode
    holder.shortCodes.text = SpannableString(shortCode).apply {
      setSpan(ForegroundColorSpan(theming.textSecondaryColor(context)), 0, shortCode.length, 0)
      setSpan(ForegroundColorSpan(theming.secondaryColor(context)), item.range.first, item.range.last + 1, 0)
    }

    holder.itemView.setOnClickListener {
      emojiSearchDialogDelegate?.onSearchEmojiClick(item.emoji)
    }
  }

  override fun getItemCount() = items.size

  fun update(
    new: List<SearchEmojiResult>,
    @Px marginStart: Int?,
  ) {
    val old = ArrayList(items)
    items = new
    this.marginStart = marginStart

    DiffUtil.calculateDiff(DiffUtilHelper(old, items) { it.emoji.hashCode() })
      .dispatchUpdatesTo(this)
  }
}

internal class EmojiViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.emoji_adapter_item_emoji_search, parent, false)) {
  val textView: EmojiTextView by lazy(LazyThreadSafetyMode.NONE) { itemView.findViewById(R.id.textView) }
  val shortCodes: TextView by lazy(LazyThreadSafetyMode.NONE) { itemView.findViewById(R.id.shortCodes) }
}

class DiffUtilHelper<T>(
  private val old: List<T>,
  private val new: List<T>,
  private val id: (T) -> Int
) : DiffUtil.Callback() {
  override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
    id(old[oldItemPosition]) == id(new[newItemPosition])

  override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
    old[oldItemPosition] == new[newItemPosition]

  override fun getOldListSize() = old.size

  override fun getNewListSize() = new.size
}
