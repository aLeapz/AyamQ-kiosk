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
import com.ayamq.kiosk.model.CartItem
import com.ayamq.kiosk.util.FormatUtils

/**
 * RecyclerView Adapter for cart items
 * Displays items in cart with quantity controls
 */
class CartAdapter(
    private val onIncrease: (CartItem) -> Unit,
    private val onDecrease: (CartItem) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view, onIncrease, onDecrease)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for cart items
     */
    class CartViewHolder(
        itemView: View,
        private val onIncrease: (CartItem) -> Unit,
        private val onDecrease: (CartItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val nameTextView: TextView = itemView.findViewById(R.id.tv_cart_item_name)
        private val priceTextView: TextView = itemView.findViewById(R.id.tv_cart_item_price)
        private val quantityTextView: TextView = itemView.findViewById(R.id.tv_quantity)
        private val increaseButton: Button = itemView.findViewById(R.id.btn_increase)
        private val decreaseButton: Button = itemView.findViewById(R.id.btn_decrease)

        fun bind(cartItem: CartItem) {
            nameTextView.text = cartItem.menuItem.name
            priceTextView.text = FormatUtils.formatPrice(cartItem.getSubtotal())
            quantityTextView.text = cartItem.quantity.toString()

            // Set click listeners
            increaseButton.setOnClickListener {
                onIncrease(cartItem)
            }

            decreaseButton.setOnClickListener {
                onDecrease(cartItem)
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    private class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.menuItem.id == newItem.menuItem.id
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            // Important: Also compare quantity to trigger updates when quantity changes
            return oldItem.menuItem == newItem.menuItem && oldItem.quantity == newItem.quantity
        }
    }
}