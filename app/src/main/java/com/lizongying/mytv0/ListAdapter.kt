package com.lizongying.mytv0

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.FOCUS_BEFORE_DESCENDANTS
import android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
import androidx.core.content.ContextCompat
import androidx.core.view.marginStart
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lizongying.mytv0.databinding.ListItemBinding
import com.lizongying.mytv0.models.TVListModel
import com.lizongying.mytv0.models.TVModel


class ListAdapter(
    private val context: Context,
    private val recyclerView: RecyclerView,
    private var listTVModel: TVListModel?,
) :
    RecyclerView.Adapter<ListAdapter.ViewHolder>() {
    private var listener: ItemListener? = null
    private var focused: View? = null
    private var defaultFocused = false
    private var defaultFocus: Int = -1

    var visiable = false

    val application = context.applicationContext as MyTVApplication

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = ListItemBinding.inflate(inflater, parent, false)

        binding.icon.layoutParams.width = application.px2Px(binding.icon.layoutParams.width)
        binding.icon.layoutParams.height = application.px2Px(binding.icon.layoutParams.height)
        binding.icon.setPadding(application.px2Px(binding.icon.paddingTop))

        val layoutParams = binding.title.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.marginStart = application.px2Px(binding.title.marginStart)
        binding.title.layoutParams = layoutParams

        binding.heart.layoutParams.width = application.px2Px(binding.heart.layoutParams.width)
        binding.heart.layoutParams.height = application.px2Px(binding.heart.layoutParams.height)

        binding.title.textSize = application.px2PxFont(binding.title.textSize)

        val layoutParamsHeart = binding.heart.layoutParams as ViewGroup.MarginLayoutParams
        layoutParamsHeart.marginStart = application.px2Px(binding.heart.marginStart)
        binding.heart.layoutParams = layoutParamsHeart

        binding.description.textSize = application.px2PxFont(binding.description.textSize)

        return ViewHolder(context, binding)
    }

    fun focusable(able: Boolean) {
        recyclerView.isFocusable = able
        recyclerView.isFocusableInTouchMode = able
        if (able) {
            recyclerView.descendantFocusability = FOCUS_BEFORE_DESCENDANTS
        } else {
            recyclerView.descendantFocusability = FOCUS_BLOCK_DESCENDANTS
        }
    }

    fun update(listTVModel: TVListModel) {
        this.listTVModel = listTVModel
        recyclerView.post {
            notifyDataSetChanged()
        }
    }

    fun clear() {
        focused?.clearFocus()
        recyclerView.invalidate()
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        listTVModel?.let {
            val tvModel = it.getTVModel(position)!!
            val view = viewHolder.itemView

            view.isFocusable = true
            view.isFocusableInTouchMode = true

            viewHolder.like(tvModel.like.value as Boolean)

            viewHolder.binding.heart.setOnClickListener {
                tvModel.setLike(!(tvModel.like.value as Boolean))
                viewHolder.like(tvModel.like.value as Boolean)
            }

            if (!defaultFocused && position == defaultFocus) {
                view.requestFocus()
                defaultFocused = true
            }

            val onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                listener?.onItemFocusChange(tvModel, hasFocus)

                if (hasFocus) {
                    viewHolder.focus(true)
                    focused = view
                    if (visiable) {
                        if (position != it.positionValue) {
                            it.setPosition(position)
                        }
                    } else {
                        visiable = true
                    }
                } else {
                    viewHolder.focus(false)
                }
            }

            view.onFocusChangeListener = onFocusChangeListener

            view.setOnClickListener { _ ->
                listener?.onItemClicked(position)
            }

            view.setOnKeyListener { _, keyCode, event: KeyEvent? ->
                if (event?.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP && position == 0) {
                        val p = getItemCount() - 1

                        (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                            p,
                            0
                        )

                        recyclerView.postDelayed({
                            val v = recyclerView.findViewHolderForAdapterPosition(p)
                            v?.itemView?.isSelected = true
                            v?.itemView?.requestFocus()
                        }, 0)
                    }

                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && position == getItemCount() - 1) {
                        val p = 0

                        (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                            p,
                            0
                        )

                        recyclerView.postDelayed({
                            val v = recyclerView.findViewHolderForAdapterPosition(p)
                            v?.itemView?.isSelected = true
                            v?.itemView?.requestFocus()
                        }, 0)
                    }

                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        tvModel.setLike(!(tvModel.like.value as Boolean))
                        viewHolder.like(tvModel.like.value as Boolean)
                    }

                    return@setOnKeyListener listener?.onKey(this, keyCode) ?: false
                }
                false
            }

            viewHolder.bindTitle(tvModel.tv.title)

            viewHolder.bindImage(tvModel.tv.logo, tvModel.tv.id)
        }
    }

    override fun getItemCount() = listTVModel?.size() ?: 0

    class ViewHolder(private val context: Context, val binding: ListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindTitle(text: String) {
            binding.title.text = text
        }

        fun bindImage(url: String?, id: Int) {
            val width = Utils.dpToPx(40)
            val height = Utils.dpToPx(40)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val paint = Paint().apply {
                color = Color.WHITE
                textSize = 32f
                textAlign = Paint.Align.CENTER
            }
            val text = String.format("%3d", id + 1)
            val x = width / 2f
            val y = height / 2f - (paint.descent() + paint.ascent()) / 2
            canvas.drawText(text, x, y, paint)

            if (url.isNullOrBlank()) {
                Glide.with(context)
                    .load(BitmapDrawable(context.resources, bitmap))
                    .centerInside()
                    .into(binding.icon)
//                binding.imageView.setImageDrawable(null)
            } else {
                Glide.with(context)
                    .load(url)
                    .centerInside()
                    .error(BitmapDrawable(context.resources, bitmap))
                    .into(binding.icon)
            }
        }

        fun focus(hasFocus: Boolean) {
            if (hasFocus) {
                binding.title.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.description.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.root.setBackgroundResource(R.color.focus)
            } else {
                binding.title.setTextColor(ContextCompat.getColor(context, R.color.title_blur))
                binding.description.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.description_blur
                    )
                )
                binding.root.setBackgroundResource(R.color.blur)
            }
        }

        fun like(liked: Boolean) {
            if (liked) {
                binding.heart.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_heart
                    )
                )
            } else {
                binding.heart.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_heart_empty
                    )
                )
            }
        }
    }

    fun toPosition(position: Int) {
        Log.i(TAG, "position $position")
        recyclerView.post {
            (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                position,
                0
            )

            recyclerView.postDelayed({
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
                viewHolder?.itemView?.isSelected = true
                viewHolder?.itemView?.requestFocus()
            }, 0)
        }
    }

    interface ItemListener {
        fun onItemFocusChange(tvModel: TVModel, hasFocus: Boolean)
        fun onItemClicked(position: Int, type: String = "list")
        fun onKey(listAdapter: ListAdapter, keyCode: Int): Boolean
    }

    fun setItemListener(listener: ItemListener) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "ListAdapter"
    }
}

