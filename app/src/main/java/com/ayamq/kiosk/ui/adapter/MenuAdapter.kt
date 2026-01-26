package com.ayamq.kiosk.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ayamq.kiosk.R
import com.ayamq.kiosk.data.database.entity.MenuEntity
import com.ayamq.kiosk.util.FormatUtils

/**
 * RecyclerView Adapter for displaying menu items
 * Uses ListAdapter with DiffUtil for efficient updates
 */
class MenuAdapter(
    private val onItemClick: (MenuEntity) -> Unit,
    private val onItemLongClick: ((MenuEntity) -> Boolean)? = null
) : ListAdapter<MenuEntity, MenuAdapter.MenuViewHolder>(MenuDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu, parent, false)
        return MenuViewHolder(view, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for menu items
     */
    class MenuViewHolder(
        itemView: View,
        private val onItemClick: (MenuEntity) -> Unit,
        private val onItemLongClick: ((MenuEntity) -> Boolean)?
    ) : RecyclerView.ViewHolder(itemView) {

        private val nameTextView: TextView = itemView.findViewById(R.id.tv_menu_name)
        private val priceTextView: TextView = itemView.findViewById(R.id.tv_menu_price)
        private val addButton: Button = itemView.findViewById(R.id.btn_add_to_cart)

        fun bind(menuItem: MenuEntity) {
            nameTextView.text = menuItem.name
            priceTextView.text = FormatUtils.formatPrice(menuItem.price)

            // Set click listener
            addButton.setOnClickListener {
                onItemClick(menuItem)
            }

            // Set long click listener (for admin delete)
            onItemLongClick?.let { longClickHandler ->
                itemView.setOnLongClickListener {
                    longClickHandler(menuItem)
                }
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     * Compares old and new items to determine what changed
     */
    private class MenuDiffCallback : DiffUtil.ItemCallback<MenuEntity>() {
        override fun areItemsTheSame(oldItem: MenuEntity, newItem: MenuEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MenuEntity, newItem: MenuEntity): Boolean {
            return oldItem == newItem
        }
    }
}