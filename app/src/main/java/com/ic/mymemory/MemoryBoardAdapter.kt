package com.ic.mymemory

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.ic.mymemory.models.BoardSize
import com.ic.mymemory.models.MemoryCard
import kotlin.math.min

// adapter class is an abstract class which means there are several methods that we need to overwrite in order for it to function

class MemoryBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    private val cardClickListener: CardClickListener
) :
    RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() { // a view holder is an object which provides access to all the views of (1 recycler view element = i memory card of game)


    companion object{ //singletance where we define constants, we can access its members directly through the containing class (equivalent to static variables)
        private const val MARGIN_SIZE = 10
        private const val TAG = "MemoryBoardAdapter"
    }

interface CardClickListener{
    fun onCardClicked(position:Int)
}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder { // figures out how to create one view of our recycler view
        val cardWidth = parent.width / boardSize.getWidth() - (2 * MARGIN_SIZE)
        val cardHeight =parent.height / boardSize.getHeight() - (2 * MARGIN_SIZE)
        val cardSideLength = min(cardWidth, cardHeight)
        val view = LayoutInflater.from(context).inflate(R.layout.memory_card, parent, false)
        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as MarginLayoutParams
        layoutParams.height = cardSideLength
        layoutParams.width = cardSideLength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) { // takes data at the position(2nd param) and binding it to the view holder (1st param)
holder.bind(position)
    }

    override fun getItemCount() = boardSize.numCards

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)


        fun bind(position: Int) {
            val memoryCard = cards[position]
            imageButton.setImageResource( if (cards[position].isFaceUp) cards[position].identifier else R.drawable.ic_launcher_background)

            imageButton.alpha =  if(memoryCard.isMatched).4f else 1.0f
//            Log.d(TAG, ContextCompat.getColorStateList(context, R.color.color_gray).toString() + "this is the opacity color")
  //          val colorStateList = if(memoryCard.isMatched)ContextCompat.getColorStateList(context, R.color.color_gray) else null
   //         ViewCompat.setBackgroundTintList(imageButton, colorStateList)

            imageButton.setOnClickListener{
    Log.i(TAG, "Clicked on position $position")
    cardClickListener.onCardClicked(position)
}
        }
    }

}
