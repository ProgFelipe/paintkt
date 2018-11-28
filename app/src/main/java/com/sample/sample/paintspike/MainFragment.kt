package com.sample.sample.paintspike

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import java.io.IOException
import kotlinx.android.synthetic.main.fragment_main.*
import com.bumptech.glide.request.target.SizeReadyCallback
import android.graphics.drawable.BitmapDrawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.BaseTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition


class MainFragment : Fragment(){

    companion object {
        @JvmStatic fun newInstance(): MainFragment = MainFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?):
            View? = inflater.inflate(R.layout.fragment_main, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        Glide.with(this)
            .asBitmap()
            .load(Uri.parse("file:///android_asset/psoriasis.jpg"))
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    img_photo.setNewImage(resource, resource)
                }
            })
        /*
            .addListener(object: RequestListener<Bitmap>{
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    img_photo.setNewImage(resource!!, resource!!)
                    return true
                }


            })*/
        /*Glide.with(this)
        .load(Uri.parse("file:///android_asset/psoriasis.jpg"))
            .asBitmap()
            .into(PaintImageView(this.context!!)){
                onresou
                @Override
                public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                    // Do something with bitmap here.
                }
            };*/
        /*Glide.with(this)
            .load(Uri.parse("file:///android_asset/psoriasis.jpg"))
            .into(img_photo)
            .waitForLayout()*/

    }

    private fun getAsset(){
        // load image
        try {
            // get input stream
            val ims = activity?.assets?.open("psoriasis.jpg")
            // load image as Drawable
            val d = Drawable.createFromStream(ims, null)
            // set image to ImageView
        } catch (ex: IOException) {
            return
        }

    }
}