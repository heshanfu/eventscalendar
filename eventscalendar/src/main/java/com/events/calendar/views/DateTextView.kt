package com.events.calendar.views

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.events.calendar.R
import com.events.calendar.utils.MeasureUtils
import com.events.calendar.utils.EventsCalendarUtil
import java.util.*

class DateTextView : View {

    private var mDateSelectListener: DateSelectListener? = null
    private var mDate: Calendar? = null
    private var mDateTextSize: Float = 0.toFloat()
    private var mDotRadius: Float = 0.toFloat()
    private var mDotX: Int = 0
    private var mDotY: Int = 0
    private var mCircleX: Int = 0
    private var mCircleY: Int = 0
    private var mDateTextX: Int = 0
    private var mDateTextY: Float = 0.toFloat()
    private var mTodayCircleradius: Float = 0.toFloat()

    private lateinit var mContext: Context
    private var mAttrs: AttributeSet? = null
    private var mDefStyleAttr: Int = 0
    private var mDefStyleRes: Int = 0

    internal var isCurrentMonth: Boolean = false
    internal var hasEvent: Boolean = false
    internal var isSelected: Boolean = false
    internal var isToday: Boolean = false
    internal var isPast: Boolean = false
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var mBgCircleRadius: Float = 0.toFloat()

    private var animate: Boolean = false    //Setting animate to 'true' will trigger repeated 'invalidate()' based on 'frameCount' resulting in Animation of SELECTION CIRCLE
    private var frameCount: Int = 0

    private var touchDown = false
    private var mDownX: Float = 0.toFloat()
    private var mDownY: Float = 0.toFloat()

    var date: Calendar
        get() = mDate!!.clone() as Calendar
        set(date) {
            mDate = date.clone() as Calendar
            invalidate()
        }

    interface DateSelectListener {
        fun onDateTextViewSelected(dateTextView: DateTextView, isClick: Boolean)
    }

    constructor(context: Context) : super(context) {
        init(context, null, -1, -1)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs, -1, -1)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr, -1)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        mContext = context
        mAttrs = attrs
        mDefStyleAttr = defStyleAttr
        mDefStyleRes = defStyleRes

        this.isClickable = true

        mDate = Calendar.getInstance()
        mDotRadius = resources.getDimension(R.dimen.radius_event_dot)

        val attributes = mContext.theme.obtainStyledAttributes(attrs, R.styleable.DateTextView, defStyleAttr, defStyleRes)
        try {
            isCurrentMonth = attributes.getBoolean(R.styleable.DateTextView_isCurrentMonth, false)
            isSelected = attributes.getBoolean(R.styleable.DateTextView_isSelected, false)
            hasEvent = attributes.getBoolean(R.styleable.DateTextView_hasEvent, false)
            isToday = attributes.getBoolean(R.styleable.DateTextView_isToday, false)
            isPast = attributes.getBoolean(R.styleable.DateTextView_isPast, false)
        } finally {
            attributes.recycle()
        }

        if (doInitializeStaticVariables) {
            selectedTextColor = EventsCalendarUtil.selectedTextColor
            selectionCircleColor = EventsCalendarUtil.selectionColor
            defaultTextColor = EventsCalendarUtil.primaryTextColor
            disabledTextColor = EventsCalendarUtil.secondaryTextColor
            eventDotColor = EventsCalendarUtil.eventDotColor

            mSelectionPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            mSelectionPaint!!.color = selectionCircleColor

            mDotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            mDotPaint?.color = eventDotColor

            mTodayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            mTodayPaint?.color = EventsCalendarUtil.primaryTextColor
            mTodayPaint?.style = Paint.Style.STROKE
            mTodayPaint!!.strokeWidth = resources.getDimension(R.dimen.width_circle_stroke)


            mDateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            mDateTextPaint?.textAlign = Paint.Align.CENTER
            mDateTextPaint?.color = EventsCalendarUtil.primaryTextColor
            mDateTextSize = mContext.resources.getDimension(R.dimen.text_calendar_date)
            mDateTextPaint?.textSize = mDateTextSize

            doInitializeStaticVariables = false
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureUtils.getMeasurement(widthMeasureSpec, mContext.resources.getDimension(R.dimen.dimen_date_text_view).toInt())
        val height = MeasureUtils.getMeasurement(heightMeasureSpec, mContext.resources.getDimension(R.dimen.dimen_date_text_view).toInt())
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h

        mDateTextSize = mHeight * (3.2f / 10f)
        mDateTextPaint!!.textSize = mDateTextSize
        mDateTextX = mWidth / 2
        mDateTextY = mHeight / 2 - (mDateTextPaint!!.ascent() + mDateTextPaint!!.descent()) / 2

        mBgCircleRadius = Math.min(mHeight - mHeight * (6f / 10f), mWidth - mWidth * (6f / 10f))
        mTodayCircleradius = mBgCircleRadius - resources.getDimension(R.dimen.width_circle_stroke) / 2
        mCircleX = mWidth / 2
        mCircleY = mHeight / 2

        mDotRadius = mHeight * (0.75f / 20f)
        mDotX = mWidth / 2
        mDotY = (mHeight * (7.5f / 10f)).toInt()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val location = IntArray(2)
        this.getLocationOnScreen(location)


        if (isPast) {
            mDateTextPaint!!.color = disabledTextColor
            canvas.drawText("" + mDate!!.get(Calendar.DATE), mDateTextX.toFloat(), mDateTextY, mDateTextPaint!!)
        } else {
            if (isCurrentMonth) {
                if (isToday) {
                    canvas.drawCircle(mCircleX.toFloat(), mCircleY.toFloat(), mTodayCircleradius, mTodayPaint!!)
                }
                if (isSelected) {
                    mDateTextPaint?.isFakeBoldText = EventsCalendarUtil.isBoldTextOnSelectionEnabled
                    mDateTextPaint?.color = selectedTextColor
                    //DRAWING SELECTION CIRCLE
                    //EDITED --> if(animate)
                    //                if (false)
                    //                {
                    //                    canvas.drawCircle(mCircleX, mCircleY, mBgCircleRadius * (frameCount / 10f), mSelectionPaint);
                    //                }
                    //                else
                    //                {
                    canvas.drawCircle(mCircleX.toFloat(), mCircleY.toFloat(), mBgCircleRadius, mSelectionPaint!!)
                    //                }
                } else {
                    mDateTextPaint?.color = defaultTextColor
                    //EDITED --> if(animate)
                    //                if (false)
                    //                {
                    //                    REMOVING SELECTION CIRCLE
                    //                    canvas.drawCircle(mCircleX, mCircleY, mBgCircleRadius * (1 - (frameCount / 10f)), mSelectionPaint);
                    //                }
                }
                canvas.drawText("" + mDate!!.get(Calendar.DATE), mDateTextX.toFloat(), mDateTextY, mDateTextPaint!!)
            } else {
                mDateTextPaint?.color = disabledTextColor
                canvas.drawText("" + mDate!!.get(Calendar.DATE), mDateTextX.toFloat(), mDateTextY, mDateTextPaint!!)
            }
        }

        super.onDraw(canvas)

        if (hasEvent) drawDot(canvas)

        if (animate) {
            frameCount++
            if (frameCount == 10) {
                animate = false
                frameCount = 0
            }
            invalidate()
        }
    }

    private fun drawDot(canvas: Canvas) {
        if (isCurrentMonth) {
            if (isSelected) {
                mDotPaint?.color = selectedTextColor
                canvas.drawCircle(mDotX.toFloat(), mDotY.toFloat(), mDotRadius, mDotPaint!!)
                mDotPaint?.color = EventsCalendarUtil.selectionColor
            } else {
                mDotPaint?.color = eventDotColor
                canvas.drawCircle(mDotX.toFloat(), mDotY.toFloat(), mDotRadius, mDotPaint!!)
            }
        } else {
            mDotPaint?.color = disabledTextColor
            canvas.drawCircle(mDotX.toFloat(), mDotY.toFloat(), mDotRadius, mDotPaint!!)
            mDotPaint?.color = EventsCalendarUtil.selectionColor
        }
    }

    fun setProperties(isCurrentMonth: Boolean, hasEvent: Boolean, isSelected: Boolean, isToday: Boolean, date: Calendar, isPast: Boolean) {
        this.isCurrentMonth = isCurrentMonth
        this.hasEvent = hasEvent
        this.isSelected = isSelected
        this.isToday = isToday
        this.isPast = isPast
        mDate = date.clone() as Calendar

        invalidate()
    }

    fun setDateClickListener(dateSelectListener: DateSelectListener) {
        mDateSelectListener = dateSelectListener
    }

    fun setIsCurrentMonth(isCurrentMonth: Boolean) {
        this.isCurrentMonth = isCurrentMonth
        invalidate()
    }

    fun setHasEvent(hasEvent: Boolean) {
        this.hasEvent = hasEvent
        invalidate()
    }

    fun setIsSelected(isSelected: Boolean) {
        this.isSelected = isSelected
        invalidate()
    }

    fun setIsToday(isToday: Boolean) {
        this.isToday = isToday
        invalidate()
    }

    fun setIsPast(isPast: Boolean) {
        this.isPast = isPast
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDown = true
                mDownX = event.x
                mDownY = event.y
            }
            MotionEvent.ACTION_UP -> if (touchDown && isPointerInsideArea(event)) if (isCurrentMonth) select(true) else select(false)
        }
        return super.onTouchEvent(event)
    }

    private fun isPointerInsideArea(event: MotionEvent): Boolean {
        touchDown = false
        val locationOnScreen = IntArray(2)
        getLocationOnScreen(locationOnScreen)
        val leftLimit = locationOnScreen[0]
        val upperLimit = locationOnScreen[1]
        val rightLimit = leftLimit + measuredWidth
        val lowerLimit = upperLimit + measuredHeight
        val x = event.x + locationOnScreen[0]
        val y = event.y + locationOnScreen[1]
        return x > leftLimit && x < rightLimit && y > upperLimit && y < lowerLimit
    }

    private fun startSelectedAnimation() {
        animate = true
        frameCount = 0
        setIsSelected(true)
    }

    private fun startUnselectedAnimation() {
        animate = true
        frameCount = 0
        setIsSelected(false)
    }

    fun select(isClick: Boolean) {
        if (!isSelected && mDateSelectListener != null) {
            if (isClick) startSelectedAnimation() else setIsSelected(true)
            mDateSelectListener!!.onDateTextViewSelected(this, isClick)
        }
    }

    fun selectOnPageChange(isClick: Boolean) {
        var isClick = isClick
        isClick = false
        if (!isSelected && mDateSelectListener != null) {
            if (isClick) startSelectedAnimation() else setIsSelected(false)
            mDateSelectListener!!.onDateTextViewSelected(this, isClick)
        }
    }

    fun unSelect(isClick: Boolean) {
        if (isClick && isCurrentMonth) startUnselectedAnimation() else setIsSelected(false)
    }

    companion object {
        private var doInitializeStaticVariables = true
        private var mDotPaint: Paint? = null
        private var mSelectionPaint: Paint? = null
        private var mTodayPaint: Paint? = null
        private var mDateTextPaint: Paint? = null
        private var selectedTextColor: Int = 0
        private var defaultTextColor: Int = 0
        private var disabledTextColor: Int = 0
        private var selectionCircleColor: Int = 0
        private var eventDotColor: Int = 0
        fun invalidateColors() {
            doInitializeStaticVariables = true
        }
    }
}
