package com.prayutsu.sckribbel.model

import com.prayutsu.sckribbel.R
import com.prayutsu.sckribbel.play.Player
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.guess_chat_text.view.*

class GuessText(var guessText: String) : Item<GroupieViewHolder>() {
    var username = ""
    var profileImageUrl = ""
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.player_guess_textView.text = guessText
        viewHolder.itemView.player_name_textView.text = username
        //load our image to star
        val uri = profileImageUrl
        val targetImageview = viewHolder.itemView.player_imageView
        Picasso.get().load(uri).into(targetImageview)
    }

    override fun getLayout(): Int {
        return R.layout.guess_chat_text
    }
}