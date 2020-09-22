package com.sjl.custombackground

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.collection.ArrayMap
import java.lang.reflect.Constructor

/**
 * LayoutFactory
 *
 * @author 林zero
 * @date 2020/9/23
 */
class LayoutFactory(private val delegate:AppCompatDelegate) : LayoutInflater.Factory2 {
    companion object {
        private val sConstructorSignature = arrayOf(Context::class.java, AttributeSet::class.java)
        private val sConstructorMap = ArrayMap<String, Constructor<out View>>()
        private val sClassPrefixList =
            arrayOf("android.widget.", "android.view.", "android.webkit.")
    }

    private val mConstructorArgs = arrayOfNulls<Any>(2)

    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        return onCreateView(name, context, attrs)
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        var view = delegate.createView(null, name, context, attrs)
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.LayoutBackground)
        try {
            if (typeArray.indexCount > 0) {
                val background = GradientDrawable()
                val color = typeArray.getColor(
                    R.styleable.LayoutBackground_layout_color,
                    Color.TRANSPARENT
                )
                val radius = typeArray.getDimension(
                    R.styleable.LayoutBackground_layout_radius,
                    0f
                )
                if (color != Color.TRANSPARENT) {
                    background.setColor(color)
                }
                if (radius != 0f) {
                    background.cornerRadius = radius
                }
                if (view == null) {
                    view = createViewFromTag(context, name, attrs)
                }
                view?.background = background
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return view
        } finally {
            typeArray.recycle()
        }

        return view
    }

    private fun createViewFromTag(context: Context, name: String, attrs: AttributeSet): View? {
        var name = name
        if (name == "view") {
            name = attrs.getAttributeValue(null, "class")
        }

        try {
            mConstructorArgs[0] = context
            mConstructorArgs[1] = attrs

            if (-1 == name.indexOf('.')) {
                for (i in sClassPrefixList.indices) {
                    val view = createViewByPrefix(context, name, sClassPrefixList[i])
                    if (view != null) {
                        return view
                    }
                }
                return null
            } else {
                return createViewByPrefix(context, name, null)
            }
        } catch (e: Exception) {
            // We do not want to catch these, lets return null and let the actual LayoutInflater
            // try
            return null
        } finally {
            // Don't retain references on context.
            mConstructorArgs[0] = null
            mConstructorArgs[1] = null
        }
    }

    @Throws(ClassNotFoundException::class, InflateException::class)
    private fun createViewByPrefix(context: Context, name: String, prefix: String?): View? {
        var constructor: Constructor<out View>? = sConstructorMap[name]
        try {
            if (constructor == null) {
                // Class not found in the cache, see if it's real, and try to add it
                val clazz = context.classLoader.loadClass(
                    if (prefix != null) prefix + name else name
                ).asSubclass(View::class.java)

                constructor = clazz.getConstructor(*sConstructorSignature)
                sConstructorMap[name] = constructor
            }
            constructor.isAccessible = true
            return constructor.newInstance(*mConstructorArgs)
        } catch (e: Exception) {
            // We do not want to catch these, lets return null and let the actual LayoutInflater
            // try
            return null
        }

    }
}