
package com.prayutsu.sckribbel.play

import androidx.recyclerview.widget.RecyclerView
import com.prayutsu.sckribbel.R
import com.prayutsu.sckribbel.model.User
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.player.*
import kotlinx.android.synthetic.main.player.view.*
import kotlinx.android.synthetic.main.player.view.player_dp_imageview

class PlayerItem(val player: User) : com.xwray.groupie.Item<GroupieViewHolder>() {

  override fun getLayout(): Int {
    return R.layout.player
  }

  override fun bind(viewHolder: GroupieViewHolder, position: Int) {

    viewHolder.itemView.player_username.text = player.username
    Picasso.get().load(player.profileImageUrl).into(viewHolder.itemView.player_dp_imageview)
  }
}