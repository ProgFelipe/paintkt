package com.sample.sample.paintspike

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.widget.SeekBar
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_main.*
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition

class MainFragment : Fragment(){

    companion object {
        @JvmStatic fun newInstance(): MainFragment = MainFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?):
            View? = inflater.inflate(R.layout.fragment_main, container, false)

    private var showFabMenuItems = true
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Glide.with(this)
            .asBitmap()
            .load(Uri.parse("file:///android_asset/psoriasis.jpg"))
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    img_photo.setNewImage(resource)
                }
            })

        fab_stroke.setOnClickListener {
            if(showFabMenuItems) {
                fab_less.show()
                fab_more.show()
                fab_restore.show()
            }else{
                fab_less.hide()
                fab_more.hide()
                fab_restore.hide()
            }
            showFabMenuItems = !showFabMenuItems
        }
        fab_less.setOnClickListener{
            img_photo.decreaseStrokeSize()
        }
        fab_more.setOnClickListener{
            img_photo.increaseStrokeSize()
        }
        fab_restore.setOnClickListener{
            img_photo.setDefaultStroke()
        }

        paint_stroke_seek_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            var progressChangedValue = 15

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                progressChangedValue = if(progress < 15){ 15 }else{ progress + 5 }
                img_photo.setStrokeValue(progressChangedValue)
                preview_circle_stroke.setCircleSize(progressChangedValue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }


    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.paint_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.erase ->  img_photo.enableEraseMode()
            R.id.draw -> img_photo.enableDrawMode()
            R.id.undo -> img_photo.onUndoDraw()
        }
        return super.onOptionsItemSelected(item)
    }
}