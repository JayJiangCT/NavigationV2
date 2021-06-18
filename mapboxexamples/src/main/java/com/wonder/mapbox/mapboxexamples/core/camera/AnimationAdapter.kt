package com.wonder.mapbox.mapboxexamples.core.camera

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.examples.core.R
import com.wonder.mapbox.mapboxexamples.core.camera.AnimationType.FastFollowing
import com.wonder.mapbox.mapboxexamples.core.camera.AnimationType.Following
import com.wonder.mapbox.mapboxexamples.core.camera.AnimationType.LookAtPOIWhenFollowing
import com.wonder.mapbox.mapboxexamples.core.camera.AnimationType.Overview
import com.wonder.mapbox.mapboxexamples.core.camera.AnimationType.RemoveRoute
import com.wonder.mapbox.mapboxexamples.core.camera.AnimationType.ToPOI
import com.wonder.mapbox.mapboxexamples.core.camera.AnimationAdapter.AnimationsViewHolder

class AnimationAdapter(
    context: Context?,
    private val callback: OnAnimationButtonClicked
) : RecyclerView.Adapter<AnimationsViewHolder>() {
    private val animationList: MutableList<AnimationType> = ArrayList()
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    init {
        animationList.add(Following)
        animationList.add(Overview)
        animationList.add(FastFollowing)
        animationList.add(ToPOI)
        animationList.add(LookAtPOIWhenFollowing)
        animationList.add(RemoveRoute)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimationsViewHolder {
        val view = inflater.inflate(R.layout.item_animation_list, parent, false)
        return AnimationsViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnimationsViewHolder, position: Int) {
        val item = animationList[position]
        holder.bindAnimations(item)
    }

    override fun getItemCount(): Int {
        return animationList.size
    }

    inner class AnimationsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var animationButton: Button = itemView.findViewById(R.id.animationButton)
        fun bindAnimations(item: AnimationType) {
            animationButton.text = animationList[adapterPosition].name
            animationButton.setOnClickListener { callback.onButtonClicked(item) }
        }
    }

    interface OnAnimationButtonClicked {
        fun onButtonClicked(animationType: AnimationType)
    }
}
