package com.prayutsu.sckribbel.play

import com.prayutsu.sckribbel.R
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.leaderboard_player.view.*

class LeaderboardItem(val player: Player) : com.xwray.groupie.Item<GroupieViewHolder>() {

    override fun getLayout(): Int {
        return R.layout.leaderboard_player
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        viewHolder.itemView.username_textview.text = player.username
        viewHolder.itemView.points_textview.text = player.points.toString()
        Picasso.get().load(player.profileImageUrl).into(viewHolder.itemView.player_photo)
    }
}