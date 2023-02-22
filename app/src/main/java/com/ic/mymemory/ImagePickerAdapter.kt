package com.ic.mymemory

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.ic.mymemory.models.BoardSize
import java.util.Collections.min
import kotlin.math.min

class ImagePickerAdapter(
    private val context: Context,
    private val ImageUris: List<Uri>,
    private val boardSize: BoardSize,
    private val imageClickListener: ImageClickListener
    ) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    interface ImageClickListener{
        fun onPlaceHolderClicked()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)
        var cardWidth = parent.width / boardSize.getWidth()
        var cardHeight = parent.height / boardSize.getHeight()
        val cardSideLength = min(cardWidth, cardHeight)
        val layoutParams = view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParams.height = cardSideLength
        layoutParams.width = cardSideLength
        return ViewHolder(view)
    }

    override fun getItemCount() = boardSize.getNumPairs()

//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//         // given position we want to display the UI
//         // 2 cases - if the position we are binding here is less than the size of the image uris -> user has picked an image for that position and we should show that image
//        // if the position we are binding here is larger than the size of the image uris -> show the default gray bg saying user still needs to pick an image
//        if(position < ImageUris.size){
//            holder.bind(ImageUris[position])
//        }else{
//            holder.bind()
//        }
//
//    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomImage)

        fun bind (uri: Uri){
            ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnClickListener(null)
        }

        fun bind (){
          ivCustomImage.setOnClickListener{
          //launch an intent for user to select photos
              imageClickListener.onPlaceHolderClicked()
        }
        }

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // given position we want to display the UI
         // 2 cases - if the position we are binding here is less than the size of the image uris -> user has picked an image for that position and we should show that image
        // if the position we are binding here is larger than the size of the image uris -> show the default gray bg saying user still needs to pick an image
        if(position < ImageUris.size){
            holder.bind(ImageUris[position])
        }else{
            holder.bind()
        }
    }

}