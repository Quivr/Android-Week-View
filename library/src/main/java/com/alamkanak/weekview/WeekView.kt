package com.alamkanak.weekview

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.appcompat.content.res.AppCompatResources
import android.text.*
import android.text.format.DateFormat
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.OverScroller
import com.alamkanak.weekview.MonthLoader.MonthChangeListener
import com.alamkanak.weekview.WeekViewUtil.daysBetween
import com.alamkanak.weekview.WeekViewUtil.getPassedMinutesInDay
import com.alamkanak.weekview.WeekViewUtil.isSameDay
import com.alamkanak.weekview.WeekViewUtil.today
import java.text.SimpleDateFormat
import java.util.*

class WeekView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    //region fields and properties
    val drawPerformanceTester = DrawPerformanceTester(false)
    private var mHomeDate: Calendar? = null
    /**
     *  the earliest day that can be displayed.  null if no minimum date is set.
     */
    var minDate: Calendar? = null
        set(value) {
            if (value === field)
                return
            if (value != null) {
                value.set(Calendar.HOUR_OF_DAY, 0)
                value.set(Calendar.MINUTE, 0)
                value.set(Calendar.SECOND, 0)
                value.set(Calendar.MILLISECOND, 0)
                if (maxDate != null && value.after(maxDate)) {
                    throw IllegalArgumentException("minDate cannot be later than maxDate")
                }
                if (field != null && WeekViewUtil.isSameDay(field!!, value))
                    return
            }
            field = value
            resetHomeDate()
            mCurrentOrigin.x = 0f
            invalidate()
        }
    /**
     *  the latest day that can be displayed. null if no maximum date is set.
     */
    var maxDate: Calendar? = null
        set(value) {
            if (field === value)
                return
            if (value != null) {
                value.set(Calendar.HOUR_OF_DAY, 0)
                value.set(Calendar.MINUTE, 0)
                value.set(Calendar.SECOND, 0)
                value.set(Calendar.MILLISECOND, 0)
                if (minDate != null && value.before(minDate)) {
                    throw IllegalArgumentException("maxDate has to be after minDate")
                }
                if (field != null && WeekViewUtil.isSameDay(field!!, value))
                    return
            }
            field = value
            resetHomeDate()
            mCurrentOrigin.x = 0f
            invalidate()
        }
    private val timeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val mHeaderWeekDayTitleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val mHeaderWeekDaySubtitleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val mHeaderWeekDayTitleTodayTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val mHeaderWeekDaySubtitleTodayTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val mEventTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG)
    private val sideTitleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val sideSubtitleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val allDaySideTitleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val mEmptyEventPaint = Paint()
    private val mHeaderBackgroundPaint: Paint = Paint()
    private val mDayBackgroundPaint: Paint = Paint()
    private val mHourSeparatorPaint = Paint()
    private val mTodayColumnBackgroundPaint: Paint = Paint()
    private val mFutureBackgroundPaint: Paint = Paint()
    private val mPastBackgroundPaint = Paint()
    private val mFutureWeekendBackgroundPaint = Paint()
    private val mPastWeekendBackgroundPaint = Paint()
    private val mNowLinePaint = Paint()
    private val mEventBackgroundPaint = Paint()
    private val mNewEventBackgroundPaint = Paint()
    private var containsAllDayEvent: Boolean = false
    private var timeTextWidth: Float = 0f
    private var timeTextHeight: Float = 0f
    var headerWeekDayTitleTextHeight: Float = 0f
        private set
    private var headerHeight: Float = 0f
    var headerWeekDaySubtitleTextHeight: Float = 0f
        private set
    private var mGestureDetector: GestureDetectorCompat? = null
    private var mScroller: OverScroller? = null
    private val mCurrentOrigin = PointF(0f, 0f)
    private var mCurrentScrollDirection = Direction.NONE
    private var widthPerDay: Float = 0f
    private var mHeaderColumnWidth: Float = 0f
    private var eventRects: MutableList<EventRect>? = null
    private val mEvents = ArrayList<WeekViewEvent>()
    private var mFetchedPeriod = -1 // the middle period the calendar has fetched.
    private var mRefreshEvents = false
    private var mCurrentFlingDirection = Direction.NONE
    private val scaleDetector = ScaleGestureDetector(context, WeekViewGestureListener())
    private var mIsZooming: Boolean = false
    /**
     *  the first visible day in the week view.
     */
    var firstVisibleDay: Calendar? = null
        private set

    private var mMinimumFlingVelocity = 0
    private var mScaledTouchSlop = 0
    private var mNewEventRect: EventRect? = null
    var textColorPicker: TextColorPicker? = null
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }
    private var mSizeOfWeekView: Float = 0f
    private var mDistanceDone = 0f
    private var mDistanceMin: Float = 0f
    private var mOffsetValueToSecureScreen = 9
    private var mStartOriginForScroll = 0f
    private var mNewHourHeight = -1
    var minHourHeight = 0
    //no minimum specified (will be dynamic, based on screen)
    private var mEffectiveMinHourHeight = minHourHeight
    //compensates for the fact that you can't keep zooming out.
    var maxHourHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 125f, resources.displayMetrics).toInt()
    var newEventIdentifier: String? = "-100"
    var newEventIconDrawable: Drawable? = null
    var newEventLengthInMinutes = 60
    var newEventTimeResolutionInMinutes = 15
    var isShowFirstDayOfWeekFirst = false
    private var mIsFirstDraw = true
    private var mAreDimensionsInvalid = true
    /**
     *  the scrolling speed factor in horizontal direction.
     */
    var xScrollingSpeed = 1f
    private var mScrollToDay: Calendar? = null
    private var mScrollToHour = -1.0
    /**
     *  corner radius for event rect (in px)
     */
    var eventCornerRadius = 0.0f
    /**
     *  whether the week view should fling horizontally.
     */
    var isHorizontalFlingEnabled = true
    /**
     *  whether the week view should fling vertically.
     */
    var isVerticalFlingEnabled = true
    /**
     *  the height of AllDay-events.
     */
    var allDayEventHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40f, resources.displayMetrics).toInt()
    /**
     * If you set this to false the `zoomFocusPoint` won't take effect any more while zooming.
     * The zoom will always be focused at the center of your gesture.
     */
    var isZoomFocusPointEnabled = true
    var scrollDuration = 250
    var timeColumnResolution = 60
    var typeface: Typeface? = Typeface.DEFAULT_BOLD
        set(value) {
            if (value == field)
                return
            if (value != null) {
                field = value
                init()
            }
        }
    var allDaySideTitleText: String? = null
        set(value) {
            if (value == field)
                return
            field = value
            invalidate()
        }

    private var mMinTime = 0
    private var mMaxTime = 24

    /**
     * auto calculate limit time on events in visible days.
     */
    var autoLimitTime = false
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    var minOverlappingMinutes = 0

    // Listeners.
    var eventClickListener: EventClickListener? = null
    var eventLongPressListener: EventLongPressListener? = null
    /**
     *  event loader in the week view. Event loaders define the  interval after which the events
     * are loaded in week view. For a MonthLoader events are loaded for every month. You can define
     * your custom event loader by extending WeekViewLoader.
     */
    val weekViewLoader: WeekViewLoader = PrefetchingWeekViewLoader(MonthLoader(object : MonthChangeListener {
        override fun onMonthChange(newYear: Int, newMonth: Int): MutableList<out WeekViewEvent>? {
            return monthChangeListener?.onMonthChange(newYear, newMonth)
        }
    }))
    var emptyViewClickListener: EmptyViewClickListener? = null
    var emptyViewLongPressListener: EmptyViewLongPressListener? = null
    var scrollListener: ScrollListener? = null
    var addEventClickListener: AddEventClickListener? = null
    var dropListener: DropListener? = null
        set(value) {
            if (field == value)
                return
            field = value
            setOnDragListener(if (value != null) DragListener() else null)
        }

    var enableDrawHeaderBackgroundOnlyOnWeekDays = false
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }
    var sideTitleText: String? = null
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }
    var sideSubtitleText: String? = null
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }
    var untitledEventText: String? = null
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            goToNearestOrigin()
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            // Check if view is zoomed.
            if (mIsZooming)
                return true

            when (mCurrentScrollDirection) {
                WeekView.Direction.NONE -> {
                    // Allow scrolling only in one direction.
                    mCurrentScrollDirection = if (Math.abs(distanceX) > Math.abs(distanceY)) {
                        if (distanceX > 0) {
                            Direction.LEFT
                        } else {
                            Direction.RIGHT
                        }
                    } else {
                        Direction.VERTICAL
                    }
                }
                WeekView.Direction.LEFT -> {
                    // Change direction if there was enough change.
                    if (Math.abs(distanceX) > Math.abs(distanceY) && distanceX < -mScaledTouchSlop) {
                        mCurrentScrollDirection = Direction.RIGHT
                    }
                }
                WeekView.Direction.RIGHT -> {
                    // Change direction if there was enough change.
                    if (Math.abs(distanceX) > Math.abs(distanceY) && distanceX > mScaledTouchSlop) {
                        mCurrentScrollDirection = Direction.LEFT
                    }
                }
                else -> {
                }
            }

            // Calculate the new origin after scroll.
            when (mCurrentScrollDirection) {
                WeekView.Direction.LEFT, WeekView.Direction.RIGHT -> {
                    val minX = xMinLimit
                    val maxX = xMaxLimit

                    mDistanceDone = if (e2.x < 0) {
                        e2.x - e1.x
                    } else {
                        e1.x - e2.x
                    }

                    when {
                        mCurrentOrigin.x - distanceX * xScrollingSpeed > maxX -> mCurrentOrigin.x = maxX
                        mCurrentOrigin.x - distanceX * xScrollingSpeed < minX -> mCurrentOrigin.x = minX
                        else -> mCurrentOrigin.x -= distanceX * xScrollingSpeed
                    }
                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                }
                WeekView.Direction.VERTICAL -> {
                    val minY = yMinLimit
                    val maxY = yMaxLimit
                    when {
                        mCurrentOrigin.y - distanceY > maxY -> mCurrentOrigin.y = maxY
                        mCurrentOrigin.y - distanceY < minY -> mCurrentOrigin.y = minY
                        else -> mCurrentOrigin.y -= distanceY
                    }
                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                }
                else -> {
                }
            }
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (mIsZooming)
                return true

            if (mCurrentFlingDirection == Direction.LEFT && !isHorizontalFlingEnabled ||
                    mCurrentFlingDirection == Direction.RIGHT && !isHorizontalFlingEnabled ||
                    mCurrentFlingDirection == Direction.VERTICAL && !isVerticalFlingEnabled) {
                return true
            }

            mScroller!!.forceFinished(true)

            mCurrentFlingDirection = mCurrentScrollDirection
            when (mCurrentFlingDirection) {
                WeekView.Direction.LEFT, WeekView.Direction.RIGHT -> if (!isScrollNumberOfVisibleDays) {
                    mScroller!!.fling(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), (velocityX * xScrollingSpeed).toInt(), 0, xMinLimit.toInt(), xMaxLimit.toInt(), yMinLimit.toInt(), yMaxLimit.toInt())
                }
                WeekView.Direction.VERTICAL -> mScroller!!.fling(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), 0, velocityY.toInt(), xMinLimit.toInt(), xMaxLimit.toInt(), yMinLimit.toInt(), yMaxLimit.toInt())
                else -> {
                }
            }

            ViewCompat.postInvalidateOnAnimation(this@WeekView)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            // If the tap was on an event then trigger the callback.
            if (eventRects != null && eventClickListener != null)
                for (eventRect in eventRects!!) {
                    if (newEventIdentifier != eventRect.event.id && eventRect.rectF != null && e.x > eventRect.rectF!!.left && e.x < eventRect.rectF!!.right && e.y > eventRect.rectF!!.top && e.y < eventRect.rectF!!.bottom) {
                        eventClickListener!!.onEventClick(eventRect.originalEvent, eventRect.rectF!!)
                        playSoundEffect(SoundEffectConstants.CLICK)
                        return super.onSingleTapConfirmed(e)
                    }
                }
            val xOffset = xStartPixel
            val x = e.x - xOffset
            val y = e.y - mCurrentOrigin.y
            // If the tap was on add new Event space, then trigger the callback
            if (addEventClickListener != null && mNewEventRect != null && mNewEventRect!!.rectF != null &&
                    mNewEventRect!!.rectF!!.contains(x, y)) {
                addEventClickListener!!.onAddEventClicked(mNewEventRect!!.event.startTime, mNewEventRect!!.event.endTime)
                return super.onSingleTapConfirmed(e)
            }
            // If the tap was on an empty space, then trigger the callback.
            if ((emptyViewClickListener != null || addEventClickListener != null) && e.x > mHeaderColumnWidth && e.y > headerHeight + weekDaysHeaderRowTotalPadding + spaceBelowAllDayEvents) {
                val selectedTime = getTimeFromPoint(e.x, e.y)
                if (selectedTime != null) {
                    val tempEvents = ArrayList(mEvents)
                    if (mNewEventRect != null) {
                        tempEvents.remove(mNewEventRect!!.event)
                        mNewEventRect = null
                    }
                    playSoundEffect(SoundEffectConstants.CLICK)
                    if (emptyViewClickListener != null)
                        emptyViewClickListener!!.onEmptyViewClicked(selectedTime.clone() as Calendar)
                    if (addEventClickListener != null) {
                        //round selectedTime to resolution
                        selectedTime.add(Calendar.MINUTE, -(newEventLengthInMinutes / 2))
                        //Fix selected time if before the minimum hour
                        if (selectedTime.get(Calendar.HOUR_OF_DAY) < mMinTime) {
                            selectedTime.set(Calendar.HOUR_OF_DAY, mMinTime)
                            selectedTime.set(Calendar.MINUTE, 0)
                        }
                        val unroundedMinutes = selectedTime.get(Calendar.MINUTE)
                        val mod = unroundedMinutes % newEventTimeResolutionInMinutes
                        selectedTime.add(Calendar.MINUTE, if (mod < Math.ceil((newEventTimeResolutionInMinutes / 2).toDouble())) -mod else newEventTimeResolutionInMinutes - mod)
                        val endTime = selectedTime.clone() as Calendar
                        //Minus one to ensure it is the same day and not midnight (next day)
                        val maxMinutes = (mMaxTime - selectedTime.get(Calendar.HOUR_OF_DAY)) * 60 - selectedTime.get(Calendar.MINUTE) - 1
                        endTime.add(Calendar.MINUTE, Math.min(maxMinutes, newEventLengthInMinutes))
                        //If clicked at end of the day, fix selected startTime
                        if (maxMinutes < newEventLengthInMinutes) {
                            selectedTime.add(Calendar.MINUTE, maxMinutes - newEventLengthInMinutes)
                        }
                        val newEvent = WeekViewEvent(newEventIdentifier!!, "", null, selectedTime, endTime)
                        val top = hourHeight * getPassedMinutesInDay(selectedTime) / 60 + eventsTop
                        val bottom = hourHeight * getPassedMinutesInDay(endTime) / 60 + eventsTop
                        // Calculate left and right.
                        val left = widthPerDay * WeekViewUtil.daysBetween(firstVisibleDay!!, selectedTime)
                        val right = left + widthPerDay
                        // Add the new event if its bounds are valid
                        if (left < right && left < width && top < height && right > mHeaderColumnWidth && bottom > 0) {
                            val dayRectF = RectF(left, top, right, bottom - mCurrentOrigin.y)
                            newEvent.color = newEventColor
                            mNewEventRect = EventRect(newEvent, newEvent, dayRectF)
                            tempEvents.add(newEvent)
                            clearEvents()
                            cacheAndSortEvents(tempEvents)
                            computePositionOfEvents(eventRects!!)
                            invalidate()
                        }
                    }
                }
            }
            return super.onSingleTapConfirmed(e)
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            if (eventLongPressListener != null && eventRects != null) {
                for (event in eventRects!!) {
                    if (event.rectF != null && e.x > event.rectF!!.left && e.x < event.rectF!!.right && e.y > event.rectF!!.top && e.y < event.rectF!!.bottom) {
                        eventLongPressListener!!.onEventLongPress(event.originalEvent, event.rectF!!)
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        return
                    }
                }
            }
            // If the tap was on in an empty space, then trigger the callback.
            if (emptyViewLongPressListener != null && e.x > mHeaderColumnWidth && e.y > headerHeight + weekDaysHeaderRowTotalPadding + spaceBelowAllDayEvents) {
                val selectedTime = getTimeFromPoint(e.x, e.y)
                if (selectedTime != null) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    emptyViewLongPressListener!!.onEmptyViewLongPress(selectedTime)
                }
            }
        }
    }

    private val numberOfPeriods: Int
        get() = ((mMaxTime - mMinTime) * (60.0 / timeColumnResolution)).toInt()

    private val yMinLimit: Float
        get() = -(((hourHeight * (mMaxTime - mMinTime)).toFloat()
                + headerHeight
                + weekDaysHeaderRowTotalPadding
                + spaceBelowAllDayEvents
                + timeTextHeight / 2) - height)

    private val yMaxLimit: Float
        get() = 0f

    private val xMinLimit: Float
        get() {
            return if (maxDate == null) {
                Integer.MIN_VALUE.toFloat()
            } else {
                val date = maxDate!!.clone() as Calendar
                date.add(Calendar.DATE, 1 - realNumberOfVisibleDays)
                while (date.before(minDate)) {
                    date.add(Calendar.DATE, 1)
                }

                getXOriginForDate(date)
            }
        }

    private val xMaxLimit: Float
        get() = if (minDate == null) {
            Integer.MAX_VALUE.toFloat()
        } else {
            getXOriginForDate(minDate!!)
        }

    private val minHourOffset: Int
        get() = hourHeight * mMinTime

    private
    val eventsTop: Float
        get() = mCurrentOrigin.y + headerHeight + weekDaysHeaderRowTotalPadding + timeTextHeight / 2 - minHourOffset

    private val leftDaysWithGaps: Int
        get() = (-Math.ceil((mCurrentOrigin.x / (widthPerDay + columnGap)).toDouble())).toInt()

    private val xStartPixel: Float
        get() = mCurrentOrigin.x + (widthPerDay + columnGap) * leftDaysWithGaps +
                mHeaderColumnWidth

    var monthChangeListener: MonthChangeListener? = null

    /**
     *  the interpreter which provides the text to show in the header column and the header row.
     */
    var dateTimeInterpreter: DateTimeInterpreter = object : DateTimeInterpreter {
        val calendar = Calendar.getInstance()
        val timeFormat = DateFormat.getTimeFormat(context)
                ?: SimpleDateFormat("HH:mm", Locale.getDefault())
        val shortDateFormat = WeekViewUtil.getWeekdayWithNumericDayAndMonthFormat(context, true)
        val normalDateFormat = WeekViewUtil.getWeekdayWithNumericDayAndMonthFormat(context, false)

        init {
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        override fun getFormattedTimeOfDay(hour: Int, minutes: Int): String {
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minutes)
            return timeFormat.format(calendar.time)
        }

        override fun getFormattedWeekDayTitle(date: Calendar): String {
            val shortDate = dayNameLength == LENGTH_SHORT
            return if (shortDate) shortDateFormat.format(date.time) else normalDateFormat.format(date.time)
        }
    }
        set(value) {
            if (field == value)
                return
            field = value
            timeFormatterCache.clear()
            initTextTimeWidth()
        }

    var weekDaySubtitleInterpreter: WeekDaySubtitleInterpreter? = null
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    /**
     *  the real number of visible days
     * If the amount of days between max date and min date is smaller, that value is returned
     */
    val realNumberOfVisibleDays: Int
        get() = if (minDate == null || maxDate == null) numberOfVisibleDays else Math.min(numberOfVisibleDays, daysBetween(minDate!!, maxDate!!) + 1)

    /**
     *  the number of visible days in a week.
     */
    var numberOfVisibleDays: Int = 3
        set(value) {
            if (field == value)
                return
            field = value
            resetHomeDate()
            mCurrentOrigin.x = 0f
            mCurrentOrigin.y = 0f
            invalidate()
        }

    var hourHeight: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, resources.displayMetrics).toInt()
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    var columnGap: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics).toInt()
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    /**
     *  the first day of the week. First day of the week is used only when the week view is first
     * drawn. It does not of any effect after user starts scrolling horizontally.
     *
     * **Note:** This method will only work if the week view is set to display more than 6 days at
     * once.
     *
     * supported values are [java.util.Calendar.SUNDAY],
     * [java.util.Calendar.MONDAY], [java.util.Calendar.TUESDAY],
     * [java.util.Calendar.WEDNESDAY], [java.util.Calendar.THURSDAY],
     * [java.util.Calendar.FRIDAY].
     */
    var firstDayOfWeek: Int = Calendar.getInstance().firstDayOfWeek
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    var textSize: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12.0f, context.resources.displayMetrics)
        set(value) {
            if (field == value)
                return
            field = value
            timeTextPaint.textSize = value
            allDaySideTitleTextPaint.textSize = value
            invalidate()
        }

    var headerWeekDayTitleTextSize: Float = textSize
        set(value) {
            if (field == value)
                return
            field = value
            mHeaderWeekDayTitleTextPaint.textSize = value
            mHeaderWeekDayTitleTodayTextPaint.textSize = value
            sideTitleTextPaint.textSize = value
            invalidate()
        }

    var headerWeekDaySubtitleTextSize: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 22.0f, context.resources.displayMetrics)
        set(value) {
            if (field == value)
                return
            field = value
            mHeaderWeekDaySubtitleTextPaint.textSize = value
            mHeaderWeekDaySubtitleTodayTextPaint.textSize = value
            sideSubtitleTextPaint.textSize = value
            invalidate()
        }

    var headerColumnPadding: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics)
        set(value) {
            if (field == value)
                return
            field = value
            mHeaderColumnWidth = timeTextWidth + headerColumnPadding * 2.0f
            invalidate()
        }

    @ColorInt
    var headerColumnTextColor: Int = Color.BLACK
        set(value) {
            if (field == value)
                return
            field = value
            mHeaderWeekDayTitleTextPaint.color = value
            mHeaderWeekDaySubtitleTextPaint.color = value
            timeTextPaint.color = value
            sideTitleTextPaint.color = value
            sideSubtitleTextPaint.color = value
            allDaySideTitleTextPaint.color = value
            invalidate()
        }

    @ColorInt
    var allDaySideTitleTextColor = headerColumnTextColor
        set(value) {
            if (field == value)
                return
            field = value
            allDaySideTitleTextPaint.color = value
            invalidate()
        }

    @ColorInt
    var headerRowBackgroundColor: Int = Color.WHITE
        set(value) {
            if (field == value)
                return
            field = value
            mHeaderBackgroundPaint.color = value
            invalidate()
        }

    @ColorInt
    var dayBackgroundColor: Int = Color.rgb(245, 245, 245)
        set(value) {
            if (field == value)
                return
            field = value
            mDayBackgroundPaint.color = value
            invalidate()
        }

    @ColorInt
    var hourSeparatorColor: Int = Color.rgb(230, 230, 230)
        set(value) {
            if (field == value)
                return
            field = value
            mHourSeparatorPaint.color = value
            invalidate()
        }

    @ColorInt
    var todayColumnBackgroundColor: Int = Color.rgb(239, 247, 254)
        set(value) {
            if (field == value)
                return
            field = value
            mTodayColumnBackgroundPaint.color = value
            invalidate()
        }

    var hourSeparatorHeight: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics).toInt()
        set(value) {
            if (field == value)
                return
            field = value
            mHourSeparatorPaint.strokeWidth = value.toFloat()
            invalidate()
        }

    @ColorInt
    var todayHeaderTextColor: Int = Color.rgb(39, 137, 228)
        set(value) {
            if (field == value)
                return
            field = value
            mHeaderWeekDayTitleTodayTextPaint.color = value
            mHeaderWeekDaySubtitleTodayTextPaint.color = value
            invalidate()
        }

    var eventTextSize: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12.0f, context.resources.displayMetrics)
        set(value) {
            if (field == value)
                return
            field = value
            mEventTextPaint.textSize = value
            invalidate()
        }

    @ColorInt
    var eventTextColor: Int = Color.BLACK
        set(value) {
            if (field == value)
                return
            field = value
            mEventTextPaint.color = value
            invalidate()
        }

    var eventPadding: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics).toInt()
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    @ColorInt
    var defaultEventColor: Int = 0
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    @ColorInt
    var newEventColor: Int = 0
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    /**
     *  the length of the day name displayed in the header row. Example of short day names is
     * 'M' for 'Monday' and example of long day names is 'Mon' for 'Monday'.
     * **Note:** Use [.setDateTimeInterpreter] instead.
     */
    var dayNameLength: Int = LENGTH_LONG
        @Deprecated("")
        set(value) {
            if (value != LENGTH_LONG && value != LENGTH_SHORT)
                throw IllegalArgumentException("length parameter must be either LENGTH_LONG or LENGTH_SHORT")
            field = value
        }

    /**
     *  the gap between overlapping events.
     */
    var overlappingEventGap: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    var weekDayHeaderRowPaddingTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics)
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    var weekDayHeaderRowPaddingBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics)
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    var weekDaysHeaderRowTotalPadding: Float = weekDayHeaderRowPaddingTop + weekDayHeaderRowPaddingBottom
        get() = weekDayHeaderRowPaddingTop + weekDayHeaderRowPaddingBottom

    var spaceBetweenWeekDaysAndAllDayEvents = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics).toInt()
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    var spaceBelowAllDayEvents = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics).toInt()
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    var spaceBetweenHeaderWeekDayTitleAndSubtitle = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, context.resources.displayMetrics).toInt()
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    /**
     * Whether weekends should have a background color different from the normal day background
     * color. The weekend background colors are defined by the attributes
     * `futureWeekendBackgroundColor` and `pastWeekendBackgroundColor`.
     */
    var isShowDistinctWeekendColor: Boolean = false
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    /**
     * Whether past and future days should have two different background colors. The past and
     * future day colors are defined by the attributes `futureBackgroundColor` and
     * `pastBackgroundColor`.
     */
    var isShowDistinctPastFutureColor: Boolean = false
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    /**
     *  whether "now" line should be displayed. "Now" line is defined by the attributes
     * `nowLineColor` and `nowLineThickness`.
     */
    var isShowNowLine: Boolean = false
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    /**
     *  the "now" line color.
     */
    @ColorInt
    var nowLineColor: Int = Color.rgb(102, 102, 102)
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    var nowLineThickness: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics).toInt()
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    /*
     *  focus point
     * 0 = top of view, 1 = bottom of view
     * The focused point (multiplier of the view height) where the week view is zoomed around.
     * This point will not move while zooming.
     */
    var zoomFocusPoint: Float = 0f
        set(value) {
            if (0 > value || value > 1)
                throw IllegalStateException("The zoom focus point percentage has to be between 0 and 1")
            field = value
        }

    @ColorInt
    var pastBackgroundColor: Int = Color.rgb(227, 227, 227)
        set(value) {
            field = value
            mPastBackgroundPaint.color = value
        }

    @ColorInt
    var futureBackgroundColor: Int = Color.rgb(245, 245, 245)
        set(value) {
            field = value
            mFutureBackgroundPaint.color = value
        }

    @ColorInt
    var pastWeekendBackgroundColor: Int = 0
        set(value) {
            field = value
            this.mPastWeekendBackgroundPaint.color = value
        }

    @ColorInt
    var futureWeekendBackgroundColor: Int = 0
        set(value) {
            field = value
            this.mFutureWeekendBackgroundPaint.color = value
        }

    var isScrollNumberOfVisibleDays: Boolean = false
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    /**
     * Get the first hour that is visible on the screen.
     *
     * @return The first hour that is visible.
     */
    val firstVisibleHour: Double
        get() = (-mCurrentOrigin.y / hourHeight).toDouble()

    var isUsingCheckersStyle: Boolean = false
        set(value) {
            if (field == value)
                return
            field = value
            invalidate()
        }

    var isSubtitleHeaderEnabled: Boolean
        get() = weekDaySubtitleInterpreter != null
        private set(value) {}

    private val timeChangedBroadcastReceiver: TimeChangedBroadcastReceiver
    private var today: Calendar = WeekViewUtil.today()
    private val containsAllDayEventCache = HashMap<Pair<SimpleDate, Int>, Boolean>()
    private val weekDayTitleFormatterCache = HashMap<SimpleDate, String>()
    private val weekDaySubtitleFormatterCache = HashMap<SimpleDate, String>()
    private val timeFormatterCache = HashMap<Pair<Int, Int>, String>()

    //endregion fields and properties

    private enum class Direction {
        NONE, LEFT, RIGHT, VERTICAL
    }

    init {
        timeChangedBroadcastReceiver = object : TimeChangedBroadcastReceiver() {
            override fun onTimeChanged() {
                this@WeekView.today = WeekViewUtil.today()
                invalidate()
            }
        }
        timeChangedBroadcastReceiver.register(context, Calendar.getInstance())
        textColorPicker = object : TextColorPicker {
            override fun getTextColor(event: WeekViewEvent): Int {
                val color = event.color
                val a = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
                return if (a < 0.2) Color.BLACK else Color.WHITE
            }
        }
        // Get the attribute values (if any).
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.WeekView, 0, 0)
        try {
            firstDayOfWeek = a.getInteger(R.styleable.WeekView_firstDayOfWeek, firstDayOfWeek)
            hourHeight = a.getDimensionPixelSize(R.styleable.WeekView_hourHeight, hourHeight)
            minHourHeight = a.getDimensionPixelSize(R.styleable.WeekView_minHourHeight, minHourHeight)
            mEffectiveMinHourHeight = minHourHeight
            maxHourHeight = a.getDimensionPixelSize(R.styleable.WeekView_maxHourHeight, maxHourHeight)
            textSize = a.getDimension(R.styleable.WeekView_textSize, textSize)
            headerColumnPadding = a.getDimension(R.styleable.WeekView_headerColumnPadding, headerColumnPadding)
            columnGap = a.getDimensionPixelSize(R.styleable.WeekView_columnGap, columnGap)
            headerColumnTextColor = a.getColor(R.styleable.WeekView_headerColumnTextColor, headerColumnTextColor)
            numberOfVisibleDays = a.getInteger(R.styleable.WeekView_noOfVisibleDays, numberOfVisibleDays)
            isShowFirstDayOfWeekFirst = a.getBoolean(R.styleable.WeekView_showFirstDayOfWeekFirst, isShowFirstDayOfWeekFirst)
            weekDayHeaderRowPaddingTop = a.getDimension(R.styleable.WeekView_weekDayHeaderRowPaddingTop, weekDayHeaderRowPaddingTop)
            weekDayHeaderRowPaddingBottom = a.getDimension(R.styleable.WeekView_weekDayHeaderRowPaddingBottom, weekDayHeaderRowPaddingBottom)
            headerRowBackgroundColor = a.getColor(R.styleable.WeekView_headerRowBackgroundColor, headerRowBackgroundColor)
            dayBackgroundColor = a.getColor(R.styleable.WeekView_dayBackgroundColor, dayBackgroundColor)
            futureBackgroundColor = a.getColor(R.styleable.WeekView_futureBackgroundColor, futureBackgroundColor)
            pastBackgroundColor = a.getColor(R.styleable.WeekView_pastBackgroundColor, pastBackgroundColor)
            // If not set, use the same color as in the week
            futureWeekendBackgroundColor = a.getColor(R.styleable.WeekView_futureWeekendBackgroundColor, futureBackgroundColor)
            pastWeekendBackgroundColor = a.getColor(R.styleable.WeekView_pastWeekendBackgroundColor, pastBackgroundColor)
            nowLineColor = a.getColor(R.styleable.WeekView_nowLineColor, nowLineColor)
            nowLineThickness = a.getDimensionPixelSize(R.styleable.WeekView_nowLineThickness, nowLineThickness)
            hourSeparatorColor = a.getColor(R.styleable.WeekView_hourSeparatorColor, hourSeparatorColor)
            todayColumnBackgroundColor = a.getColor(R.styleable.WeekView_todayColumnBackgroundColor, todayColumnBackgroundColor)
            hourSeparatorHeight = a.getDimensionPixelSize(R.styleable.WeekView_hourSeparatorHeight, hourSeparatorHeight)
            todayHeaderTextColor = a.getColor(R.styleable.WeekView_todayHeaderTextColor, todayHeaderTextColor)
            eventTextSize = a.getDimension(R.styleable.WeekView_eventTextSize, eventTextSize)
            eventTextColor = a.getColor(R.styleable.WeekView_eventTextColor, eventTextColor)
            newEventColor = a.getColor(R.styleable.WeekView_newEventColor, newEventColor)
            newEventIconDrawable = a.getDrawable(R.styleable.WeekView_newEventIconResource)
            // For backward compatibility : Set "mNewEventIdentifier" if the attribute is "WeekView_newEventId" of type int
            newEventIdentifier = a.getString(R.styleable.WeekView_newEventIdentifier) ?: newEventIdentifier
            newEventLengthInMinutes = a.getInt(R.styleable.WeekView_newEventLengthInMinutes, newEventLengthInMinutes)
            newEventTimeResolutionInMinutes = a.getInt(R.styleable.WeekView_newEventTimeResolutionInMinutes, newEventTimeResolutionInMinutes)
            eventPadding = a.getDimensionPixelSize(R.styleable.WeekView_eventPadding, eventPadding)
            dayNameLength = a.getInteger(R.styleable.WeekView_dayNameLength, dayNameLength)
            overlappingEventGap = a.getDimension(R.styleable.WeekView_overlappingEventGap, overlappingEventGap)
            spaceBetweenWeekDaysAndAllDayEvents = a.getDimensionPixelSize(R.styleable.WeekView_spaceBetweenWeekDaysAndAllDayEvents, spaceBetweenWeekDaysAndAllDayEvents)
            xScrollingSpeed = a.getFloat(R.styleable.WeekView_xScrollingSpeed, xScrollingSpeed)
            eventCornerRadius = a.getDimension(R.styleable.WeekView_eventCornerRadius, eventCornerRadius)
            isShowDistinctPastFutureColor = a.getBoolean(R.styleable.WeekView_showDistinctPastFutureColor, isShowDistinctPastFutureColor)
            isShowDistinctWeekendColor = a.getBoolean(R.styleable.WeekView_showDistinctWeekendColor, isShowDistinctWeekendColor)
            isShowNowLine = a.getBoolean(R.styleable.WeekView_showNowLine, isShowNowLine)
            isHorizontalFlingEnabled = a.getBoolean(R.styleable.WeekView_horizontalFlingEnabled, isHorizontalFlingEnabled)
            isVerticalFlingEnabled = a.getBoolean(R.styleable.WeekView_verticalFlingEnabled, isVerticalFlingEnabled)
            allDayEventHeight = a.getDimensionPixelSize(R.styleable.WeekView_allDayEventHeight, allDayEventHeight)
            zoomFocusPoint = a.getFraction(R.styleable.WeekView_zoomFocusPoint, 1, 1, zoomFocusPoint)
            isZoomFocusPointEnabled = a.getBoolean(R.styleable.WeekView_zoomFocusPointEnabled, isZoomFocusPointEnabled)
            scrollDuration = a.getInt(R.styleable.WeekView_scrollDuration, scrollDuration)
            timeColumnResolution = a.getInt(R.styleable.WeekView_timeColumnResolution, timeColumnResolution)
            autoLimitTime = a.getBoolean(R.styleable.WeekView_autoLimitTime, autoLimitTime)
            mMinTime = a.getInt(R.styleable.WeekView_minTime, mMinTime)
            mMaxTime = a.getInt(R.styleable.WeekView_maxTime, mMaxTime)
            minOverlappingMinutes = a.getInt(R.styleable.WeekView_minOverlappingMinutes, minOverlappingMinutes)
            isScrollNumberOfVisibleDays = a.getBoolean(R.styleable.WeekView_isScrollNumberOfVisibleDays, isScrollNumberOfVisibleDays)
            enableDrawHeaderBackgroundOnlyOnWeekDays = a.getBoolean(R.styleable.WeekView_enableDrawHeaderBackgroundOnlyOnWeekDays, enableDrawHeaderBackgroundOnlyOnWeekDays)
            isUsingCheckersStyle = a.getBoolean(R.styleable.WeekView_isUsingCheckersStyle, isUsingCheckersStyle)
            headerWeekDayTitleTextSize = a.getDimension(R.styleable.WeekView_headerWeekDayTitleTextSize, headerWeekDayTitleTextSize)
            headerWeekDaySubtitleTextSize = a.getDimension(R.styleable.WeekView_headerWeekDaySubtitleTextSize, headerWeekDaySubtitleTextSize)
            spaceBetweenHeaderWeekDayTitleAndSubtitle = a.getDimensionPixelSize(R.styleable.WeekView_spaceBetweenHeaderWeekDayTitleAndSubtitle, spaceBetweenHeaderWeekDayTitleAndSubtitle)
            untitledEventText = a.getString(R.styleable.WeekView_untitledEventText) ?: untitledEventText
        } finally {
            a.recycle()
        }
        //some one time initializations
        mHeaderWeekDayTitleTextPaint.textAlign = Paint.Align.CENTER
        mHeaderWeekDaySubtitleTextPaint.textAlign = Paint.Align.CENTER
        sideTitleTextPaint.textAlign = Paint.Align.CENTER
        sideSubtitleTextPaint.textAlign = Paint.Align.CENTER
        allDaySideTitleTextPaint.textAlign = Paint.Align.CENTER
        timeTextPaint.textAlign = Paint.Align.RIGHT
        mEventTextPaint.style = Paint.Style.FILL
        mHourSeparatorPaint.style = Paint.Style.STROKE
        mHeaderWeekDayTitleTodayTextPaint.textAlign = Paint.Align.CENTER
        mHeaderWeekDaySubtitleTodayTextPaint.textAlign = Paint.Align.CENTER
        init()
    }

    private fun init() {
        resetHomeDate()

        // Scrolling initialization.
        mGestureDetector = GestureDetectorCompat(context, mGestureListener)
        mScroller = OverScroller(context, FastOutLinearInInterpolator())

        mMinimumFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
        mScaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

        // Measure settings for time column.
        timeTextPaint.textSize = textSize
        timeTextPaint.color = headerColumnTextColor
        timeTextPaint.typeface = typeface

        val rect = Rect()
        val exampleTime = if (timeColumnResolution % 60 != 0) "00:00 PM" else "00 PM"
        timeTextPaint.getTextBounds(exampleTime, 0, exampleTime.length, rect)
        timeTextHeight = rect.height().toFloat()
        initTextTimeWidth()

        //handle sideTitleTextPaint
        sideTitleTextPaint.color = headerColumnTextColor
        sideTitleTextPaint.typeface = typeface
        sideTitleTextPaint.textSize = headerWeekDayTitleTextSize
        // handle sideSubtitleTextPaint
        sideSubtitleTextPaint.textSize = headerWeekDaySubtitleTextSize
        sideSubtitleTextPaint.color = headerColumnTextColor
        sideSubtitleTextPaint.typeface = typeface

        //handle allDaySideTitleTextPaint
        allDaySideTitleTextPaint.textSize = textSize
        allDaySideTitleTextPaint.color = allDaySideTitleTextColor
        allDaySideTitleTextPaint.typeface = typeface

        // Measure settings for header row.
        //TODO measure the text that will actually be used, based on the locale and dates. Important because various characters might look different.
        val sampleText = "ABCDEFGHIKLMNOPQRSTUVWXYZabcdefghiklmnopqrstuvwxyz0123456789"
        mHeaderWeekDayTitleTextPaint.color = headerColumnTextColor
        mHeaderWeekDayTitleTextPaint.textSize = headerWeekDayTitleTextSize
        mHeaderWeekDayTitleTextPaint.typeface = typeface
        mHeaderWeekDayTitleTextPaint.getTextBounds(sampleText, 0, sampleText.length, rect)
        headerWeekDayTitleTextHeight = rect.height().toFloat()

        //measure settings for header subtitle
        mHeaderWeekDaySubtitleTextPaint.color = headerColumnTextColor
        mHeaderWeekDaySubtitleTextPaint.textSize = headerWeekDaySubtitleTextSize
        mHeaderWeekDaySubtitleTextPaint.typeface = typeface
        mHeaderWeekDaySubtitleTextPaint.getTextBounds(sampleText, 0, sampleText.length, rect)
        headerWeekDaySubtitleTextHeight = rect.height().toFloat()

        // Prepare header background paint.
        mHeaderBackgroundPaint.color = headerRowBackgroundColor

        // Prepare day background color paint.
        mDayBackgroundPaint.color = dayBackgroundColor
        mFutureBackgroundPaint.color = futureBackgroundColor
        mPastBackgroundPaint.color = pastBackgroundColor
        mFutureWeekendBackgroundPaint.color = futureWeekendBackgroundColor
        mPastWeekendBackgroundPaint.color = pastWeekendBackgroundColor

        // Prepare hour separator color paint.
        mHourSeparatorPaint.strokeWidth = hourSeparatorHeight.toFloat()
        mHourSeparatorPaint.color = hourSeparatorColor

        // Prepare the "now" line color paint
        mNowLinePaint.strokeWidth = nowLineThickness.toFloat()
        mNowLinePaint.color = nowLineColor

        // Prepare today background color paint.
        mTodayColumnBackgroundPaint.color = todayColumnBackgroundColor

        // Prepare today header text color paint.

        mHeaderWeekDayTitleTodayTextPaint.textSize = headerWeekDayTitleTextSize
        mHeaderWeekDayTitleTodayTextPaint.typeface = typeface
        mHeaderWeekDayTitleTodayTextPaint.color = todayHeaderTextColor

        mHeaderWeekDaySubtitleTodayTextPaint.textSize = headerWeekDaySubtitleTextSize
        mHeaderWeekDaySubtitleTodayTextPaint.typeface = typeface
        mHeaderWeekDaySubtitleTodayTextPaint.color = todayHeaderTextColor

        // Prepare event background color.
        mEventBackgroundPaint.color = Color.rgb(174, 208, 238)
        // Prepare empty event background color.
        mNewEventBackgroundPaint.color = Color.rgb(60, 147, 217)

        // Prepare event text size and color.
        mEventTextPaint.color = eventTextColor
        mEventTextPaint.textSize = eventTextSize
        mEventTextPaint.typeface = typeface


        // Set default event color.
        defaultEventColor = 0xff9fc6e7.toInt()
        // Set default empty event color.
        newEventColor = 0xff3c93d9.toInt()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        context.unregisterReceiver(timeChangedBroadcastReceiver)
    }

    private fun resetHomeDate() {
        var newHomeDate = today()

        if (minDate != null && newHomeDate.before(minDate)) {
            newHomeDate = minDate!!.clone() as Calendar
        }
        if (maxDate != null && newHomeDate.after(maxDate)) {
            newHomeDate = maxDate!!.clone() as Calendar
        }

        if (maxDate != null) {
            val date = maxDate!!.clone() as Calendar
            date.add(Calendar.DATE, 1 - realNumberOfVisibleDays)
            while (date.before(minDate)) {
                date.add(Calendar.DATE, 1)
            }

            if (newHomeDate.after(date)) {
                newHomeDate = date
            }
        }

        mHomeDate = newHomeDate
    }

    private fun getXOriginForDate(date: Calendar): Float {
        return -daysBetween(mHomeDate!!, date) * (widthPerDay + columnGap)
    }

    // fix rotation changes
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mAreDimensionsInvalid = true
    }

    /**
     * Initialize time column width. Calculate value with all possible hours (supposed widest text).
     */
    private fun initTextTimeWidth() {
        timeTextWidth = 0f
        for (i in 0 until numberOfPeriods) {
            // Measure time string and get max width.
            val time = getFormattedTime(i, i % 2 * 30)
            timeTextWidth = Math.max(timeTextWidth, timeTextPaint.measureText(time))
        }
        mHeaderColumnWidth = timeTextWidth + headerColumnPadding * 2.0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawPerformanceTester.startMeasure()
        // Draw the header row.
        drawHeaderRowAndEvents(canvas)
        // Draw the time column and all the axes/separators.
        drawTimeColumnAndAxes(canvas)
        drawPerformanceTester.endMeasure()
    }

    private fun calculateHeaderHeight() {
        //Make sure the header is the right size (depends on AllDay events)
        if (eventRects != null && eventRects!!.size > 0) {
            val cacheKey = Pair(SimpleDate(firstVisibleDay!!), realNumberOfVisibleDays)
            val cachedResult = containsAllDayEventCache[cacheKey]
            if (cachedResult != null)
                containsAllDayEvent = cachedResult
            else {
                containsAllDayEvent = false
                val day = firstVisibleDay!!.clone() as Calendar
                outerLoop@
                for (dayNumber in 0 until realNumberOfVisibleDays) {
                    for (i in eventRects!!.indices) {
                        val event = eventRects!![i].event
                        if (isSameDay(event.startTime, day) && event.isAllDay) {
                            containsAllDayEvent = true
                            break@outerLoop
                        }
                    }
                    day.add(Calendar.DATE, 1)
                }
                containsAllDayEventCache[cacheKey] = containsAllDayEvent
            }
        }
        headerHeight = if (containsAllDayEvent) {
            headerWeekDayTitleTextHeight + (allDayEventHeight + spaceBelowAllDayEvents + spaceBetweenWeekDaysAndAllDayEvents)
        } else {
            headerWeekDayTitleTextHeight
        }
        if (isSubtitleHeaderEnabled)
            headerHeight += headerWeekDaySubtitleTextHeight + spaceBetweenHeaderWeekDayTitleAndSubtitle
    }

    private fun drawTimeColumnAndAxes(canvas: Canvas) {
        canvas.save()
        // Clip to paint in left column only.
        canvas.clipRect(0.0f, headerHeight + weekDaysHeaderRowTotalPadding, mHeaderColumnWidth, height.toFloat())

        for (i in 0 until numberOfPeriods) {
            // If we are showing half hours (eg. 5:30am), space the times out by half the hour height
            // and need to provide 30 minutes on each odd period, otherwise, minutes is always 0.
            val timeSpacing: Float
            val minutes: Int
            val hour: Int
            val timesPerHour = 60.0f / timeColumnResolution
            timeSpacing = hourHeight / timesPerHour
            hour = mMinTime + i / timesPerHour.toInt()
            minutes = i % timesPerHour.toInt() * (60 / timesPerHour.toInt())
            // Calculate the top of the rectangle where the time text will go
            val top = headerHeight + weekDaysHeaderRowTotalPadding + mCurrentOrigin.y + timeSpacing * i + spaceBelowAllDayEvents
            // Get the time to be displayed, as a String.
            val time = getFormattedTime(hour, minutes)
            // Draw the text if its y position is not outside of the visible area. The pivot point of the text is the point at the bottom-right corner.
            if (top < height)
                canvas.drawText(time, timeTextWidth + headerColumnPadding, top + timeTextHeight, timeTextPaint)
        }
        canvas.restore()
    }

    private fun drawHeaderRowAndEvents(canvas: Canvas) {
        // Calculate the available width for each day.
        widthPerDay = (width.toFloat() - mHeaderColumnWidth - (columnGap.toFloat() * (realNumberOfVisibleDays.toFloat() - 1.0f))) / realNumberOfVisibleDays.toFloat()
        calculateHeaderHeight() //Make sure the header is the right size (depends on AllDay events)

        if (mAreDimensionsInvalid) {
            mEffectiveMinHourHeight = Math.max(minHourHeight, ((height.toFloat() - headerHeight - weekDaysHeaderRowTotalPadding - spaceBelowAllDayEvents) / (mMaxTime - mMinTime)).toInt())

            mAreDimensionsInvalid = false
            if (mScrollToDay != null)
                goToDate(mScrollToDay!!)

            mAreDimensionsInvalid = false
            if (mScrollToHour >= 0)
                goToHour(mScrollToHour)

            mScrollToDay = null
            mScrollToHour = -1.0
            mAreDimensionsInvalid = false
        }
        if (mIsFirstDraw) {
            mIsFirstDraw = false

            // If the week view is being drawn for the first time, then consider the first day of the week.
            if (realNumberOfVisibleDays >= 7 && mHomeDate!!.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek && isShowFirstDayOfWeekFirst) {
                val difference = mHomeDate!!.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek
                mCurrentOrigin.x += (widthPerDay + columnGap) * difference
            }
            setLimitTime(mMinTime, mMaxTime)
        }

        // Calculate the new height due to the zooming.
        if (mNewHourHeight > 0) {
            if (mNewHourHeight < mEffectiveMinHourHeight)
                mNewHourHeight = mEffectiveMinHourHeight
            else if (mNewHourHeight > maxHourHeight)
                mNewHourHeight = maxHourHeight

            hourHeight = mNewHourHeight
            mNewHourHeight = -1
        }

        // If the new mCurrentOrigin.y is invalid, make it valid.
        mCurrentOrigin.y = Math.min(0f, Math.max(mCurrentOrigin.y,
                height.toFloat() - (hourHeight * (mMaxTime - mMinTime)).toFloat() - headerHeight - weekDaysHeaderRowTotalPadding - spaceBelowAllDayEvents - timeTextHeight / 2))

        val leftDaysWithGaps = leftDaysWithGaps
        // Consider scroll offset.
        val startFromPixel = xStartPixel
        var startPixel = startFromPixel

        // Prepare to iterate for each hour to draw the hour lines.
        var lineCount = ((height.toFloat() - headerHeight - weekDaysHeaderRowTotalPadding - spaceBelowAllDayEvents) / hourHeight).toInt() + 1
        lineCount *= (realNumberOfVisibleDays + 1)

        val hourLines = FloatArray(lineCount * 4)

        // Clear the cache for event rectangles.
        if (eventRects != null)
            for (eventRect in eventRects!!)
                eventRect.rectF = null

        canvas.save()

        // Clip to paint events only.
        canvas.clipRect(mHeaderColumnWidth, headerHeight + weekDaysHeaderRowTotalPadding + spaceBelowAllDayEvents + timeTextHeight / 2, width.toFloat(), height.toFloat())

        // Iterate through each day.

        val oldFirstVisibleDay = firstVisibleDay
        firstVisibleDay = mHomeDate!!.clone() as Calendar
        firstVisibleDay!!.add(Calendar.DATE, -Math.round(mCurrentOrigin.x / (widthPerDay + columnGap)))

        if (oldFirstVisibleDay == null || !WeekViewUtil.isSameDay(firstVisibleDay!!, oldFirstVisibleDay)) {
            scrollListener?.onFirstVisibleDayChanged(firstVisibleDay!!, oldFirstVisibleDay)
        }

        if (autoLimitTime) {
            val days = ArrayList<Calendar>()
            for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + realNumberOfVisibleDays) {
                val day = mHomeDate!!.clone() as Calendar
                day.add(Calendar.DATE, dayNumber - 1)
                days.add(day)
            }
            limitEventTime(days)
        }
        run {
            val day = mHomeDate!!.clone() as Calendar
            day.add(Calendar.DATE, leftDaysWithGaps)

            for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + realNumberOfVisibleDays + 1) {
                // Check if the day is today.
                val isToday = isSameDay(day, today)

                // Don't draw days which are outside requested range
                if (!dateIsValid(day))
                    continue

                // Get more events if necessary.

                // mFetchedPeriod: currently fetched period index
                // mWeekViewLoader.toWeekViewPeriodIndex(day): index for the day we want to display
                // fetchIndex = 1.0: end of period in the future reached
                // fetchIndex = 0.0: end of period in the past reached
                val fetchIndex = this.weekViewLoader.toWeekViewPeriodIndex(day) - mFetchedPeriod

                // if we are using the PrefetchingWeekViewLoader class, we need to adjust the bounds
                // so that we wait to fetch new data until we really need it
                var upperBound = 1.0
                var lowerBound = 0.0

                if (this.weekViewLoader is PrefetchingWeekViewLoader) {
                    // the offset causes the onMonthChangeListener to be trigger when half of the
                    // last fetched period is passed

                    // example:
                    // if the prefetching period = 1, we load the current period, the next and the previous
                    // when half of the next/previous period is passed, the listener is triggered to fetch new data
                    val boundOffset = this.weekViewLoader.prefetchingPeriod - 0.5

                    upperBound = 1.0 + boundOffset
                    lowerBound = 0.0 - boundOffset
                }

                if ((eventRects == null || mRefreshEvents ||
                                dayNumber == leftDaysWithGaps + 1 && mFetchedPeriod != this.weekViewLoader.toWeekViewPeriodIndex(day).toInt() &&
                                (fetchIndex >= upperBound || fetchIndex <= lowerBound))) {
                    getMoreEvents(day)
                    mRefreshEvents = false
                }

                // Draw background color for each day.
                val start = if (startPixel < mHeaderColumnWidth) mHeaderColumnWidth else startPixel
                if (widthPerDay + startPixel - start > 0) {
                    if (isShowDistinctPastFutureColor) {
                        val isWeekend = day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                        val pastPaint = if (isWeekend && isShowDistinctWeekendColor) mPastWeekendBackgroundPaint else mPastBackgroundPaint
                        val futurePaint = if (isWeekend && isShowDistinctWeekendColor) mFutureWeekendBackgroundPaint else mFutureBackgroundPaint
                        val startY = headerHeight + weekDaysHeaderRowTotalPadding + timeTextHeight / 2 + spaceBelowAllDayEvents + mCurrentOrigin.y
                        when {
                            isToday -> {
                                val now = Calendar.getInstance()
                                val beforeNow = (now.get(Calendar.HOUR_OF_DAY) - mMinTime + now.get(Calendar.MINUTE) / 60f) * hourHeight
                                canvas.drawRect(start, startY, startPixel + widthPerDay, startY + beforeNow, pastPaint)
                                canvas.drawRect(start, startY + beforeNow, startPixel + widthPerDay, height.toFloat(), futurePaint)
                            }
                            day.before(today) -> canvas.drawRect(start, startY, startPixel + widthPerDay, height.toFloat(), pastPaint)
                            else -> canvas.drawRect(start, startY, startPixel + widthPerDay, height.toFloat(), futurePaint)
                        }
                    } else {
                        val cellBackgroundPaint = if (isToday) mTodayColumnBackgroundPaint else mDayBackgroundPaint
                        if (cellBackgroundPaint.color != 0)
                            canvas.drawRect(start, headerHeight + weekDaysHeaderRowTotalPadding + timeTextHeight / 2 + spaceBelowAllDayEvents, startPixel + widthPerDay, height.toFloat(), cellBackgroundPaint)
                    }
                }

                // Prepare the separator lines for hours.
                var i = 0
                for (hourNumber in mMinTime until mMaxTime) {
                    val top = headerHeight + weekDaysHeaderRowTotalPadding + mCurrentOrigin.y + (hourHeight * (hourNumber - mMinTime)).toFloat() + timeTextHeight / 2
                    if (top > headerHeight + weekDaysHeaderRowTotalPadding + timeTextHeight / 2 + spaceBelowAllDayEvents - hourSeparatorHeight && top < height && startPixel + widthPerDay - start > 0) {
                        hourLines[i * 4] = start
                        hourLines[i * 4 + 1] = top
                        hourLines[i * 4 + 2] = startPixel + widthPerDay + if (isUsingCheckersStyle) columnGap else 0
                        hourLines[i * 4 + 3] = top
                        i++
                    }
                }
                // Draw the lines for hours.
                canvas.drawLines(hourLines, mHourSeparatorPaint)

                // Draw line between days (before current one)
                if (isUsingCheckersStyle) {
                    val x = if (dayNumber == leftDaysWithGaps + 1) start else start - columnGap / 2
                    canvas.drawLine(x, headerHeight, x, height.toFloat(), mHourSeparatorPaint)
                }

                // Draw the events.
                drawEvents(day, startPixel, canvas)

                // Draw the line at the current time.
                if (isShowNowLine && isToday) {
                    val startY = headerHeight + weekDaysHeaderRowTotalPadding + timeTextHeight / 2 + spaceBelowAllDayEvents + mCurrentOrigin.y
                    val now = Calendar.getInstance()
                    val beforeNow = (now.get(Calendar.HOUR_OF_DAY) - mMinTime + now.get(Calendar.MINUTE) / 60f) * hourHeight
                    val top = startY + beforeNow
                    canvas.drawLine(start, top, startPixel + widthPerDay, top, mNowLinePaint)
                }

                // In the next iteration, start from the next day.
                startPixel += widthPerDay + columnGap
                day.add(Calendar.DATE, 1)
            }
        }

        canvas.restore()

        // Hide everything in the first cell (top left corner).
        canvas.save()
        canvas.clipRect(0f, 0f, mHeaderColumnWidth, headerHeight + weekDaysHeaderRowTotalPadding)
        val headerTitleAndSubtitleTextHeight = headerWeekDayTitleTextHeight + (if (isSubtitleHeaderEnabled) headerWeekDaySubtitleTextHeight + spaceBetweenHeaderWeekDayTitleAndSubtitle else 0.0f)
        if (enableDrawHeaderBackgroundOnlyOnWeekDays)
            canvas.drawRect(0f, 0f, mHeaderColumnWidth, headerTitleAndSubtitleTextHeight + weekDaysHeaderRowTotalPadding, mHeaderBackgroundPaint)
        else
            canvas.drawRect(canvas.clipBounds, mHeaderBackgroundPaint)

        // draw text on the left of the week days
        when {
        //TODO set left column size based on possible text of sideTitle and sideSubtitle, or auto-resize text according to available space
            !TextUtils.isEmpty(sideTitleText) && TextUtils.isEmpty(sideSubtitleText) ->
                canvas.drawText(sideTitleText, mHeaderColumnWidth / 2, (headerTitleAndSubtitleTextHeight + headerWeekDayTitleTextHeight) / 2.0f + weekDayHeaderRowPaddingTop, sideTitleTextPaint)
            !TextUtils.isEmpty(sideTitleText) && !TextUtils.isEmpty(sideSubtitleText) -> {
                canvas.drawText(sideTitleText, mHeaderColumnWidth / 2, headerWeekDayTitleTextHeight + weekDayHeaderRowPaddingTop, sideTitleTextPaint)
                canvas.drawText(sideSubtitleText, mHeaderColumnWidth / 2, headerTitleAndSubtitleTextHeight + weekDayHeaderRowPaddingTop, sideSubtitleTextPaint)
            }
            TextUtils.isEmpty(sideTitleText) && !TextUtils.isEmpty(sideSubtitleText) ->
                canvas.drawText(sideSubtitleText, mHeaderColumnWidth / 2, (headerTitleAndSubtitleTextHeight + sideSubtitleTextPaint.textSize) / 2.0f + weekDayHeaderRowPaddingTop, sideSubtitleTextPaint)
        }

        canvas.restore()
        // Clip to paint header row only.
        canvas.save()
        canvas.clipRect(mHeaderColumnWidth, 0f, width.toFloat(), headerHeight + weekDaysHeaderRowTotalPadding)


        // Draw the header background.
        if (enableDrawHeaderBackgroundOnlyOnWeekDays)
            canvas.drawRect(0f, 0f, width.toFloat(), headerTitleAndSubtitleTextHeight + weekDaysHeaderRowTotalPadding, mHeaderBackgroundPaint)
        else
            canvas.drawRect(0f, 0f, width.toFloat(), headerHeight + weekDaysHeaderRowTotalPadding, mHeaderBackgroundPaint)

        canvas.restore()
        canvas.save()

        canvas.clipRect(mHeaderColumnWidth, 0f, width.toFloat(), headerHeight + weekDaysHeaderRowTotalPadding - spaceBelowAllDayEvents)

        // Draw the header row texts.
        run {
            val day = mHomeDate!!.clone() as Calendar
            startPixel = startFromPixel
            day.add(Calendar.DATE, leftDaysWithGaps)
            for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + realNumberOfVisibleDays + 1) {
                // Check if the day is today.
                val isToday = isSameDay(day, today)
                // Don't draw days which are outside requested range
                if (!dateIsValid(day)) {
                    day.add(Calendar.DAY_OF_YEAR, 1)
                    continue
                }
                // Draw the day labels title
                val dayLabel = getFormattedWeekDayTitle(day)
                canvas.drawText(dayLabel, startPixel + widthPerDay / 2, headerWeekDayTitleTextHeight + weekDayHeaderRowPaddingTop, if (isToday) mHeaderWeekDayTitleTodayTextPaint else mHeaderWeekDayTitleTextPaint)

                //draw day subtitle
                if (isSubtitleHeaderEnabled) {
                    val subtitleText = getFormattedWeekDaySubtitle(day)
                    canvas.drawText(subtitleText, startPixel + widthPerDay / 2, headerTitleAndSubtitleTextHeight + weekDayHeaderRowPaddingTop,
                            if (isToday) mHeaderWeekDaySubtitleTodayTextPaint else mHeaderWeekDaySubtitleTextPaint)
                }
                if (containsAllDayEvent)
                    drawAllDayEvents(day, startPixel, canvas)
                startPixel += widthPerDay + columnGap
                day.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        canvas.restore()
        //draw text on the left of the all-day events
        if (containsAllDayEvent && !TextUtils.isEmpty(allDaySideTitleText)) {
            canvas.save()
            val weekDaysHeight = headerTitleAndSubtitleTextHeight + weekDaysHeaderRowTotalPadding
            val top = weekDaysHeight + spaceBetweenWeekDaysAndAllDayEvents + timeTextHeight / 2
            val bottom = top + allDayEventHeight
            canvas.clipRect(0f, 0f, top, bottom)
            canvas.drawText(allDaySideTitleText, mHeaderColumnWidth / 2, (top + bottom) / 2, allDaySideTitleTextPaint)
            canvas.restore()
        }
    }

    private fun getFormattedTime(hour: Int, minutes: Int): String {
        val cacheKey = Pair(hour, minutes)
        val cachedResult = timeFormatterCache[cacheKey]
        if (cachedResult != null)
            return cachedResult
        val result = dateTimeInterpreter.getFormattedTimeOfDay(hour, minutes)
        timeFormatterCache[cacheKey] = result
        return result
    }

    private fun getFormattedWeekDayTitle(cal: Calendar): String {
        val cacheKey = SimpleDate(cal)
        val cachedResult = weekDayTitleFormatterCache[cacheKey]
        if (cachedResult != null)
            return cachedResult
        val result = dateTimeInterpreter.getFormattedWeekDayTitle(cal)
        weekDayTitleFormatterCache[cacheKey] = result
        return result
    }

    private fun getFormattedWeekDaySubtitle(cal: Calendar): String {
        val cacheKey = SimpleDate(cal)
        val cachedResult = weekDaySubtitleFormatterCache[cacheKey]
        if (cachedResult != null)
            return cachedResult
        val result = weekDaySubtitleInterpreter!!.getFormattedWeekDaySubtitle(cal)
        weekDaySubtitleFormatterCache[cacheKey] = result
        return result
    }

    /**
     * Get the time and date where the user clicked on.
     *
     * @param x The x position of the touch event.
     * @param y The y position of the touch event.
     * @return The time and date at the clicked position.
     */
    private fun getTimeFromPoint(x: Float, y: Float): Calendar? {
        val leftDaysWithGaps = leftDaysWithGaps
        var startPixel = xStartPixel
        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + realNumberOfVisibleDays + 1) {
            val start = if (startPixel < mHeaderColumnWidth) mHeaderColumnWidth else startPixel
            if (widthPerDay + startPixel - start > 0 && x > start && x < startPixel + widthPerDay) {
                val day = mHomeDate!!.clone() as Calendar
                day.add(Calendar.DATE, dayNumber - 1)
                val pixelsFromZero = (y - mCurrentOrigin.y - headerHeight
                        - weekDaysHeaderRowTotalPadding - timeTextHeight / 2 - spaceBelowAllDayEvents)
                val hour = (pixelsFromZero / hourHeight).toInt()
                val minute = (60 * (pixelsFromZero - hour * hourHeight) / hourHeight).toInt()
                day.add(Calendar.HOUR_OF_DAY, hour + mMinTime)
                day.set(Calendar.MINUTE, minute)
                return day
            }
            startPixel += widthPerDay + columnGap
        }
        return null
    }

    /**
     * limit current time of event by update mMinTime & mMaxTime
     * find smallest of start time & latest of end time
     */
    private fun limitEventTime(dates: MutableList<Calendar>) {
        if (eventRects != null && eventRects!!.size > 0) {
            var startTime: Calendar? = null
            var endTime: Calendar? = null

            for (eventRect in eventRects!!) {
                for (date in dates) {
                    if (isSameDay(eventRect.event.startTime, date) && !eventRect.event.isAllDay) {

                        if (startTime == null || getPassedMinutesInDay(startTime) > getPassedMinutesInDay(eventRect.event.startTime)) {
                            startTime = eventRect.event.startTime
                        }

                        if (endTime == null || getPassedMinutesInDay(endTime) < getPassedMinutesInDay(eventRect.event.endTime)) {
                            endTime = eventRect.event.endTime
                        }
                    }
                }
            }

            if (startTime != null && endTime != null && startTime.before(endTime)) {
                setLimitTime(Math.max(0, startTime.get(Calendar.HOUR_OF_DAY)),
                        Math.min(24, endTime.get(Calendar.HOUR_OF_DAY) + 1))
                return
            }
        }
    }

    /**
     * Draw all the events of a particular day.
     *
     * @param date           The day.
     * @param startFromPixel The left position of the day area. The events will never go any left from this value.
     * @param canvas         The canvas to draw upon.
     */
    private fun drawEvents(date: Calendar, startFromPixel: Float, canvas: Canvas) {
        if (eventRects == null || eventRects!!.isEmpty())
            return
        for (eventRect in eventRects!!) {
            if (isSameDay(eventRect.event.startTime, date) && !eventRect.event.isAllDay) {
                val top = hourHeight * eventRect.top / 60 + eventsTop
                val bottom = hourHeight * eventRect.bottom / 60 + eventsTop

                // Calculate left and right.
                var left = startFromPixel + eventRect.left * widthPerDay
                if (left < startFromPixel)
                    left += overlappingEventGap
                var right = left + eventRect.width * widthPerDay
                if (right < startFromPixel + widthPerDay)
                    right -= overlappingEventGap

                // Draw the event and the event name on top of it.
                if (left < right && left < width && top < height && right > mHeaderColumnWidth &&
                        bottom > headerHeight + weekDaysHeaderRowTotalPadding + timeTextHeight / 2 + spaceBelowAllDayEvents) {
                    eventRect.rectF = RectF(left, top, right, bottom)
                    mEventBackgroundPaint.color = if (eventRect.event.color == 0) defaultEventColor else eventRect.event.color
                    mEventBackgroundPaint.shader = eventRect.event.shader
                    canvas.drawRoundRect(eventRect.rectF!!, eventCornerRadius, eventCornerRadius, mEventBackgroundPaint)
                    var topToUse = top
                    if (eventRect.event.startTime.get(Calendar.HOUR_OF_DAY) < mMinTime)
                        topToUse = hourHeight * getPassedMinutesInDay(mMinTime, 0) / 60 + eventsTop

                    if (newEventIdentifier != eventRect.event.id)
                        drawEventTitle(eventRect.event, eventRect.rectF!!, canvas, topToUse, left)
                    else
                        drawEmptyImage(eventRect.event, eventRect.rectF!!, canvas, topToUse, left)

                } else
                    eventRect.rectF = null
            }
        }
    }

    /**
     * Draw all the Allday-events of a particular day.
     *
     * @param date           The day.
     * @param startFromPixel The left position of the day area. The events will never go any left from this value.
     * @param canvas         The canvas to draw upon.
     */
    private fun drawAllDayEvents(date: Calendar, startFromPixel: Float, canvas: Canvas) {
        if (eventRects == null || eventRects!!.isEmpty())
            return
        val headerTitleAndSubtitleTextHeight = headerWeekDayTitleTextHeight + (if (isSubtitleHeaderEnabled) headerWeekDaySubtitleTextHeight + spaceBetweenHeaderWeekDayTitleAndSubtitle else 0.0f)
        for (eventRect in eventRects!!) {
            if (isSameDay(eventRect.event.startTime, date) && eventRect.event.isAllDay) {
                // Calculate top.
                val weekDaysHeight = headerTitleAndSubtitleTextHeight + weekDaysHeaderRowTotalPadding
                val top = weekDaysHeight + spaceBetweenWeekDaysAndAllDayEvents + timeTextHeight / 2
                // Calculate bottom.
                val bottom = top + eventRect.bottom
                // Calculate left and right.
                var left = startFromPixel + eventRect.left * widthPerDay
                if (left < startFromPixel)
                    left += overlappingEventGap
                var right = left + eventRect.width * widthPerDay
                if (right < startFromPixel + widthPerDay)
                    right -= overlappingEventGap
                // Draw the event and the event name on top of it.
                if (left < right && left < width && top < height && right > mHeaderColumnWidth && bottom > 0) {
                    eventRect.rectF = RectF(left, top, right, bottom)
                    mEventBackgroundPaint.color = if (eventRect.event.color == 0) defaultEventColor else eventRect.event.color
                    mEventBackgroundPaint.shader = eventRect.event.shader
                    canvas.drawRoundRect(eventRect.rectF!!, eventCornerRadius, eventCornerRadius, mEventBackgroundPaint)
                    drawEventTitle(eventRect.event, eventRect.rectF!!, canvas, top, left)
                } else
                    eventRect.rectF = null
            }
        }
    }

    /**
     * Draw the name of the event on top of the event rectangle.
     *
     * @param event        The event of which the title (and location) should be drawn.
     * @param rect         The rectangle on which the text is to be drawn.
     * @param canvas       The canvas to draw upon.
     * @param originalTop  The original top position of the rectangle. The rectangle may have some of its portion outside of the visible area.
     * @param originalLeft The original left position of the rectangle. The rectangle may have some of its portion outside of the visible area.
     */
    private fun drawEventTitle(event: WeekViewEvent, rect: RectF, canvas: Canvas, originalTop: Float, originalLeft: Float) {
        if (rect.right - rect.left - (eventPadding * 2).toFloat() < 0) return
        if (rect.bottom - rect.top - (eventPadding * 2).toFloat() < 0) return

        // Prepare the name of the event.
        val bob = SpannableStringBuilder()
        if (!TextUtils.isEmpty(event.name) || !TextUtils.isEmpty(untitledEventText)) {
            if (!TextUtils.isEmpty(event.name))
                bob.append(event.name)
            else bob.append(untitledEventText)
            bob.setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, bob.length, 0)
        }
        // Prepare the location of the event.
        if (!TextUtils.isEmpty(event.location)) {
            if (bob.isNotEmpty())
                bob.append(' ')
            bob.append(event.location)
        }

        val availableHeight = (rect.bottom - originalTop - (eventPadding * 2).toFloat()).toInt()
        val availableWidth = (rect.right - originalLeft - (eventPadding * 2).toFloat()).toInt()

        // Get text color if necessary
        if (textColorPicker != null) {
            mEventTextPaint.color = textColorPicker!!.getTextColor(event)
        }
        // Get text dimensions.
        var textLayout = StaticLayout(bob, mEventTextPaint, availableWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, false)
        if (textLayout.lineCount > 0) {
            val lineHeight = textLayout.height / textLayout.lineCount

            if (availableHeight >= lineHeight) {
                // Calculate available number of line counts.
                var availableLineCount = availableHeight / lineHeight
                do {
                    // Ellipsize text to fit into event rect.
                    if (newEventIdentifier != event.id)
                        textLayout = StaticLayout(TextUtils.ellipsize(bob, mEventTextPaint, (availableLineCount * availableWidth).toFloat(), TextUtils.TruncateAt.END), mEventTextPaint, (rect.right - originalLeft - (eventPadding * 2).toFloat()).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, false)

                    // Reduce line count.
                    availableLineCount--

                    // Repeat until text is short enough.
                } while (textLayout.height > availableHeight)

                // Draw text.
                canvas.save()
                canvas.translate(originalLeft + eventPadding, originalTop + eventPadding)
                textLayout.draw(canvas)
                canvas.restore()
            }
        }
    }

    /**
     * Draw the text on top of the rectangle in the empty event.
     */
    private fun drawEmptyImage(event: WeekViewEvent, rect: RectF, canvas: Canvas, originalTop: Float, originalLeft: Float) {
        val size = Math.max(1, Math.floor(Math.min(0.8 * rect.height(), 0.8 * rect.width())).toInt())
        if (newEventIconDrawable == null)
            newEventIconDrawable = AppCompatResources.getDrawable(context, android.R.drawable.ic_input_add)
        var icon = (newEventIconDrawable as BitmapDrawable).bitmap
        icon = Bitmap.createScaledBitmap(icon, size, size, false)
        canvas.drawBitmap(icon, originalLeft + (rect.width() - icon.width) / 2, originalTop + (rect.height() - icon.height) / 2, mEmptyEventPaint)
    }

    /**
     * A class to hold reference to the events and their visual representation. An EventRect is
     * actually the rectangle that is drawn on the calendar for a given event. There may be more
     * than one rectangle for a single event (an event that expands more than one day). In that
     * case two instances of the EventRect will be used for a single event. The given event will be
     * stored in "originalEvent". But the event that corresponds to rectangle the rectangle
     * instance will be stored in "event".
     */
    /**
     * Create a new instance of event rect. An EventRect is actually the rectangle that is drawn
     * on the calendar for a given event. There may be more than one rectangle for a single
     * event (an event that expands more than one day). In that case two instances of the
     * EventRect will be used for a single event. The given event will be stored in
     * "originalEvent". But the event that corresponds to rectangle the rectangle instance will
     * be stored in "event".
     *
     * @param event         Represents the event which this instance of rectangle represents.
     * @param originalEvent The original event that was passed by the user.
     * @param rectF         The rectangle.
     */
    private inner class EventRect(var event: WeekViewEvent, var originalEvent: WeekViewEvent, var rectF: RectF?) {
        var left: Float = 0f
        var width: Float = 0f
        var top: Float = 0f
        var bottom: Float = 0f
        override fun toString(): String {
            return "EventRect(left=$left, width=$width, top=$top, bottom=$bottom, rectF=$rectF, event=$event, originalEvent=$originalEvent)"
        }
    }

    /**
     * Gets more events of one/more month(s) if necessary. This method is called when the user is
     * scrolling the week view. The week view stores the events of three months: the visible month,
     * the previous month, the next month.
     *
     * @param day The day where the user is currently is.
     */
    private fun getMoreEvents(day: Calendar) {
        clearOptimizationsCaches()
        // Get more events if the month is changed.
        if (eventRects == null)
            eventRects = ArrayList()
        // If a refresh was requested then reset some variables.
        if (mRefreshEvents) {
            this.clearEvents()
            mFetchedPeriod = -1
        }
        val periodToFetch = this.weekViewLoader.toWeekViewPeriodIndex(day).toInt()
        if (!isInEditMode && (mFetchedPeriod < 0 || mFetchedPeriod != periodToFetch || mRefreshEvents)) {
            val newEvents = this.weekViewLoader.onLoad(periodToFetch)
            // Clear events.
            this.clearEvents()
            cacheAndSortEvents(newEvents)
            calculateHeaderHeight()
            mFetchedPeriod = periodToFetch
        }
        // Prepare to calculate positions of each events.
        val tempEvents = eventRects
        eventRects = ArrayList()
        // Iterate through each day with events to calculate the position of the events.
        while (tempEvents!!.size > 0) {
            val eventRects = ArrayList<EventRect>(tempEvents.size)
            // Get first event for a day.
            val eventRect1 = tempEvents.removeAt(0)
            eventRects.add(eventRect1)
            var i = 0
            while (i < tempEvents.size) {
                // Collect all other events for same day.
                val eventRect2 = tempEvents[i]
                if (isSameDay(eventRect1.event.startTime, eventRect2.event.startTime)) {
                    tempEvents.removeAt(i)
                    eventRects.add(eventRect2)
                } else {
                    i++
                }
            }
            computePositionOfEvents(eventRects)
        }
    }

    private fun clearEvents() {
        clearOptimizationsCaches()
        eventRects!!.clear()
        mEvents.clear()
    }

    private fun clearOptimizationsCaches() {
        containsAllDayEventCache.clear()
        timeFormatterCache.clear()
        weekDayTitleFormatterCache.clear()
        weekDaySubtitleFormatterCache.clear()
    }

    /**
     * Cache the event for smooth scrolling functionality.
     *
     * @param event The event to cache.
     */
    private fun cacheEvent(event: WeekViewEvent) {
        if (!event.isAllDay && event.startTime >= event.endTime)
            return
        val splitEvents = event.splitWeekViewEvents()
        for (splitEvent in splitEvents) {
            eventRects!!.add(EventRect(splitEvent, event, null))
        }
        mEvents.add(event)
    }

    /**
     * Cache and sort events.
     *
     * @param events The events to be cached and sorted.
     */
    private fun cacheAndSortEvents(events: MutableList<out WeekViewEvent>?) {
        if (events != null)
            for (event in events)
                cacheEvent(event)
        sortEventRects(eventRects)
    }

    /**
     * Sorts the events in ascending order.
     *
     * @param eventRects The events to be sorted.
     */
    private fun sortEventRects(eventRects: MutableList<EventRect>?) {
        eventRects?.sortWith(Comparator { left, right ->
            val start1 = left.event.startTime.timeInMillis
            val start2 = right.event.startTime.timeInMillis
            var comparator = if (start1 > start2) 1 else if (start1 < start2) -1 else 0
            if (comparator == 0) {
                val end1 = left.event.endTime.timeInMillis
                val end2 = right.event.endTime.timeInMillis
                comparator = if (end1 > end2) 1 else if (end1 < end2) -1 else 0
            }
            comparator
        })
    }

    /**
     * Calculates the left and right positions of each events. This comes handy specially if events
     * are overlapping.
     *
     * @param eventRects The events along with their wrapper class.
     */
    private fun computePositionOfEvents(eventRects: MutableList<EventRect>) {
        // Make "collision groups" for all events that collide with others.
        val collisionGroups = ArrayList<ArrayList<EventRect>>()
        for (eventRect in eventRects) {
            var isPlaced = false
            outerLoop@
            for (collisionGroup in collisionGroups)
                for (groupEvent in collisionGroup) {
                    if (isEventsCollide(groupEvent.event, eventRect.event)) { //&& groupEvent.event.isAllDay == eventRect.event.isAllDay) {
                        collisionGroup.add(eventRect)
                        isPlaced = true
                        break@outerLoop
                    }
                }
            if (!isPlaced) {
                val newGroup = ArrayList<EventRect>()
                newGroup.add(eventRect)
                collisionGroups.add(newGroup)
            }
        }
        for (collisionGroup in collisionGroups)
            expandEventsToMaxWidth(collisionGroup)
    }

    /**
     * Expands all the events to maximum possible width. The events will try to occupy maximum
     * space available horizontally.
     *
     * @param collisionGroup The group of events which overlap with each other.
     */
    private fun expandEventsToMaxWidth(collisionGroup: MutableList<EventRect>) {
        // Expand the events to maximum possible width.
        val columns = ArrayList<ArrayList<EventRect>>()
        columns.add(ArrayList())
        for (eventRect in collisionGroup) {
            var isPlaced = false
            for (column in columns) {
                if (column.size == 0) {
                    column.add(eventRect)
                    isPlaced = true
                } else if (!isEventsCollide(eventRect.event, column[column.size - 1].event)) {
                    column.add(eventRect)
                    isPlaced = true
                    break
                }
            }
            if (!isPlaced) {
                val newColumn = ArrayList<EventRect>()
                newColumn.add(eventRect)
                columns.add(newColumn)
            }
        }

        // Calculate left and right position for all the events.
        // Get the maxRowCount by looking in all columns.
        var maxRowCount = 0
        for (column in columns) {
            maxRowCount = Math.max(maxRowCount, column.size)
        }
        for (i in 0 until maxRowCount) {
            // Set the left and right values of the event.
            var j = 0f
            for (column in columns) {
                if (column.size >= i + 1) {
                    val eventRect = column[i]
                    eventRect.width = 1f / columns.size
                    eventRect.left = j / columns.size
                    if (!eventRect.event.isAllDay) {
                        eventRect.top = getPassedMinutesInDay(eventRect.event.startTime).toFloat()
                        eventRect.bottom = getPassedMinutesInDay(eventRect.event.endTime).toFloat()
                    } else {
                        eventRect.top = 0f
                        eventRect.bottom = allDayEventHeight.toFloat()
                    }
                    eventRects!!.add(eventRect)
                }
                j++
            }
        }
    }

    /**
     * Checks if two events overlap.
     *
     * @param event1 The first event.
     * @param event2 The second event.
     * @return true if the events overlap.
     */
    private fun isEventsCollide(event1: WeekViewEvent, event2: WeekViewEvent): Boolean {
        if (event1.isAllDay != event2.isAllDay)
            return false
        val start1 = event1.startTime.timeInMillis
        val start2 = event2.startTime.timeInMillis
        val end1 = event1.endTime.timeInMillis
        val end2 = event2.endTime.timeInMillis
        if (event1.isAllDay)
            return !(start1 > end2 || end1 < start2)
        val minOverlappingMillis = (minOverlappingMinutes * 60 * 1000).toLong()
        return !(start1 + minOverlappingMillis >= end2 || end1 <= start2 + minOverlappingMillis)
    }


    /**
     * Checks if time1 occurs after (or at the same time) time2.
     *
     * @param time1 The time to check.
     * @param time2 The time to check against.
     * @return true if time1 and time2 are equal or if time1 is after time2. Otherwise false.
     */
    private fun isTimeAfterOrEquals(time1: Calendar?, time2: Calendar?): Boolean {
        return !(time1 == null || time2 == null) && time1.timeInMillis >= time2.timeInMillis
    }

    override fun invalidate() {
        super.invalidate()
        mAreDimensionsInvalid = true
    }

/////////////////////////////////////////////////////////////////
//
//      Functions related to setting and getting the properties.
//
/////////////////////////////////////////////////////////////////

    private fun recalculateHourHeight() {
        val height = ((height - (headerHeight + weekDaysHeaderRowTotalPadding + timeTextHeight / 2 + spaceBelowAllDayEvents)) / (this.mMaxTime - this.mMinTime)).toInt()
        if (height > hourHeight) {
            if (height > maxHourHeight)
                maxHourHeight = height
            mNewHourHeight = height
        }
    }

    /**
     * Set visible time span.
     *
     * @param startHour limit time display on top (between 0~24)
     * @param endHour   limit time display at bottom (between 0~24 and larger than startHour)
     */
    fun setLimitTime(startHour: Int, endHour: Int) {
        when {
            endHour <= startHour -> throw IllegalArgumentException("endHour must larger startHour.")
            startHour < 0 -> throw IllegalArgumentException("startHour must be at least 0.")
            endHour > 24 -> throw IllegalArgumentException("endHour can't be higher than 24.")
            else -> {
                this.mMinTime = startHour
                this.mMaxTime = endHour
                recalculateHourHeight()
                invalidate()
            }
        }
    }

    /**
     * Set minimal shown time
     *
     * @param startHour limit time display on top (between 0~24) and smaller than endHour
     */
    fun setMinTime(startHour: Int) {
        if (mMaxTime <= startHour) {
            throw IllegalArgumentException("startHour must smaller than endHour")
        } else if (startHour < 0) {
            throw IllegalArgumentException("startHour must be at least 0.")
        }
        this.mMinTime = startHour
        recalculateHourHeight()
    }

    /**
     * Set highest shown time
     *
     * @param endHour limit time display at bottom (between 0~24 and larger than startHour)
     */
    fun setMaxTime(endHour: Int) {
        if (endHour <= mMinTime) {
            throw IllegalArgumentException("endHour must be larger than startHour.")
        } else if (endHour > 24) {
            throw IllegalArgumentException("endHour can't be higher than 24.")
        }
        this.mMaxTime = endHour
        recalculateHourHeight()
        invalidate()
    }

/////////////////////////////////////////////////////////////////
//
//      Functions related to scrolling.
//
/////////////////////////////////////////////////////////////////

    override fun onTouchEvent(event: MotionEvent): Boolean {

        mSizeOfWeekView = (widthPerDay + columnGap) * numberOfVisibleDays
        mDistanceMin = mSizeOfWeekView / mOffsetValueToSecureScreen

        scaleDetector.onTouchEvent(event)
        val value = mGestureDetector!!.onTouchEvent(event)

        // Check after call of mGestureDetector, so mCurrentFlingDirection and mCurrentScrollDirection are set.
        if (event.action == MotionEvent.ACTION_UP && !mIsZooming && mCurrentFlingDirection == Direction.NONE) {
            if (mCurrentScrollDirection == Direction.RIGHT || mCurrentScrollDirection == Direction.LEFT) {
                goToNearestOrigin()
            }
            mCurrentScrollDirection = Direction.NONE
        }

        return value
    }

    private fun goToNearestOrigin() {
        var leftDays = (mCurrentOrigin.x / (widthPerDay + columnGap)).toDouble()

        val beforeScroll = mStartOriginForScroll
        var isPassed = false

        if (mDistanceDone > mDistanceMin || mDistanceDone < -mDistanceMin || !isScrollNumberOfVisibleDays) {

            when {
                !isScrollNumberOfVisibleDays && mCurrentFlingDirection != Direction.NONE -> // snap to nearest day
                    leftDays = Math.round(leftDays).toDouble()
                mCurrentScrollDirection == Direction.LEFT -> {
                    // snap to last day
                    leftDays = Math.floor(leftDays)
                    mStartOriginForScroll -= mSizeOfWeekView
                    isPassed = true
                }
                mCurrentScrollDirection == Direction.RIGHT -> {
                    // snap to next day
                    leftDays = Math.floor(leftDays)
                    mStartOriginForScroll += mSizeOfWeekView
                    isPassed = true
                }
                else -> // snap to nearest day
                    leftDays = Math.round(leftDays).toDouble()
            }


            if (isScrollNumberOfVisibleDays) {
                val mayScrollHorizontal = beforeScroll - mStartOriginForScroll < xMaxLimit && mCurrentOrigin.x - mStartOriginForScroll > xMinLimit
                if (isPassed && mayScrollHorizontal) {
                    // Stop current animation.
                    mScroller!!.forceFinished(true)
                    // Snap to date.
                    if (mCurrentScrollDirection == Direction.LEFT) {
                        mScroller!!.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), (beforeScroll - mCurrentOrigin.x - mSizeOfWeekView).toInt(), 0, 200)
                    } else if (mCurrentScrollDirection == Direction.RIGHT) {
                        mScroller!!.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), (mSizeOfWeekView - (mCurrentOrigin.x - beforeScroll)).toInt(), 0, 200)
                    }
                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                }
            } else {
                val nearestOrigin = (mCurrentOrigin.x - leftDays * (widthPerDay + columnGap)).toInt()
                val mayScrollHorizontal = mCurrentOrigin.x - nearestOrigin < xMaxLimit && mCurrentOrigin.x - nearestOrigin > xMinLimit
                if (mayScrollHorizontal) {
                    mScroller!!.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), -nearestOrigin, 0)
                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                }

                if (nearestOrigin != 0 && mayScrollHorizontal) {
                    // Stop current animation.
                    mScroller!!.forceFinished(true)
                    // Snap to date.
                    mScroller!!.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), -nearestOrigin, 0, (Math.abs(nearestOrigin) / widthPerDay * scrollDuration).toInt())
                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                }
            }

            // Reset scrolling and fling direction.
            mCurrentFlingDirection = Direction.NONE
            mCurrentScrollDirection = mCurrentFlingDirection


        } else {
            mScroller!!.forceFinished(true)
            if (mCurrentScrollDirection == Direction.LEFT) {
                mScroller!!.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), beforeScroll.toInt() - mCurrentOrigin.x.toInt(), 0, 200)
            } else if (mCurrentScrollDirection == Direction.RIGHT) {
                mScroller!!.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), beforeScroll.toInt() - mCurrentOrigin.x.toInt(), 0, 200)
            }
            ViewCompat.postInvalidateOnAnimation(this@WeekView)

            // Reset scrolling and fling direction.
            mCurrentFlingDirection = Direction.NONE
            mCurrentScrollDirection = mCurrentFlingDirection
        }
    }

    override fun computeScroll() {
        super.computeScroll()

        if (mScroller!!.isFinished) {
            if (mCurrentFlingDirection != Direction.NONE) {
                // Snap to day after fling is finished.
                goToNearestOrigin()
            }
        } else {
            if (mCurrentFlingDirection != Direction.NONE && forceFinishScroll()) {
                goToNearestOrigin()
            } else if (mScroller!!.computeScrollOffset()) {
                mCurrentOrigin.y = mScroller!!.currY.toFloat()
                mCurrentOrigin.x = mScroller!!.currX.toFloat()
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }
    }

    /**
     * Check if scrolling should be stopped.
     *
     * @return true if scrolling should be stopped before reaching the end of animation.
     */
    private fun forceFinishScroll(): Boolean {
        return mScroller!!.currVelocity <= mMinimumFlingVelocity
    }


    /////////////////////////////////////////////////////////////////
//
//      Public methods.
//
/////////////////////////////////////////////////////////////////
    fun getLastVisibleDay(): Calendar? {
        if (firstVisibleDay == null)
            return null
        val result = firstVisibleDay!!.clone() as Calendar
        result.add(Calendar.DATE, realNumberOfVisibleDays - 1)
        return result
    }

    /**
     * Show today on the week view.
     */
    fun goToToday() {
        val today = Calendar.getInstance()
        goToDate(today)
    }

    /**
     * Show a specific day on the week view.
     *
     * @param date The date to show.
     */
    fun goToDate(date: Calendar) {
        mScroller!!.forceFinished(true)
        mCurrentFlingDirection = Direction.NONE
        mCurrentScrollDirection = mCurrentFlingDirection

        date.set(Calendar.HOUR_OF_DAY, 0)
        date.set(Calendar.MINUTE, 0)
        date.set(Calendar.SECOND, 0)
        date.set(Calendar.MILLISECOND, 0)

        if (mAreDimensionsInvalid) {
            mScrollToDay = date
            return
        }

        mRefreshEvents = true

        mCurrentOrigin.x = -daysBetween(mHomeDate!!, date) * (widthPerDay + columnGap)
        mStartOriginForScroll = mCurrentOrigin.x
        invalidate()
    }

    /**
     * Refreshes the view and loads the events again.
     */
    fun notifyDataSetChanged() {
        mRefreshEvents = true
        invalidate()
    }

    /**
     * Vertically scroll to a specific hour in the week view.
     *
     * @param hour The hour to scroll to in 24-hour format. Supported values are 0-24.
     */
    fun goToHour(hour: Double) {
        if (mAreDimensionsInvalid) {
            mScrollToHour = hour
            return
        }

        var verticalOffset = 0
        if (hour > mMaxTime)
            verticalOffset = hourHeight * (mMaxTime - mMinTime)
        else if (hour > mMinTime)
            verticalOffset = (hourHeight * hour).toInt()

        if (verticalOffset > (hourHeight * (mMaxTime - mMinTime) - height).toFloat() + headerHeight + weekDaysHeaderRowTotalPadding + spaceBelowAllDayEvents)
            verticalOffset = ((hourHeight * (mMaxTime - mMinTime) - height).toFloat() + headerHeight + weekDaysHeaderRowTotalPadding + spaceBelowAllDayEvents).toInt()

        mCurrentOrigin.y = (-verticalOffset).toFloat()
        invalidate()
    }

    /**
     * Determine whether a given calendar day falls within the scroll limits set for this view.
     *
     * @param day the day to check
     * @return True if there are no limit or the date is within the limits.
     * @see .setMinDate
     * @see .setMaxDate
     */
    fun dateIsValid(day: Calendar): Boolean {
        if (minDate != null && day.before(minDate))
            return false
        return !(maxDate != null && day.after(maxDate))
    }

//region interfaces

    interface DropListener {
        /**
         * Triggered when view dropped
         *
         * @param view: dropped view.
         * @param date: object set with the date and time of the dropped coordinates on the view.
         */
        fun onDrop(view: View, date: Calendar)
    }

    interface EventClickListener {
        /**
         * Triggered when clicked on one existing event
         *
         * @param event:     event clicked.
         * @param eventRect: view containing the clicked event.
         */
        fun onEventClick(event: WeekViewEvent, eventRect: RectF)
    }

    interface EventLongPressListener {
        fun onEventLongPress(event: WeekViewEvent, eventRect: RectF)
    }

    interface EmptyViewClickListener {
        /**
         * Triggered when the users clicks on a empty space of the calendar.
         *
         * @param date: [Calendar] object set with the date and time of the clicked position on the view.
         */
        fun onEmptyViewClicked(date: Calendar)

    }

    interface EmptyViewLongPressListener {
        /**
         * Similar to [com.alamkanak.weekview.WeekView.EmptyViewClickListener] but with long press.
         *
         * @param time: [Calendar] object set with the date and time of the long pressed position on the view.
         */
        fun onEmptyViewLongPress(time: Calendar)
    }

    interface ScrollListener {
        /**
         * Called when the first visible day has changed.
         *
         *
         * (this will also be called during the first draw of the weekview)
         *
         * @param newFirstVisibleDay The new first visible day
         * @param oldFirstVisibleDay The old first visible day (is null on the first call).
         */
        fun onFirstVisibleDayChanged(newFirstVisibleDay: Calendar, oldFirstVisibleDay: Calendar?)
    }

    interface AddEventClickListener {
        /**
         * Triggered when the users clicks to create a new event.
         *
         * @param startTime The startTime of a new event
         * @param endTime   The endTime of a new event
         */
        fun onAddEventClicked(startTime: Calendar, endTime: Calendar)
    }

    /**
     * A simple GestureListener that holds the focused hour while scaling.
     */
    private inner class WeekViewGestureListener : ScaleGestureDetector.OnScaleGestureListener {
        internal var mFocusedPointY: Float = 0f

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            mIsZooming = false
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mIsZooming = true
            goToNearestOrigin()

            // Calculate focused point for scale action
            mFocusedPointY = if (isZoomFocusPointEnabled) {
                // Use fractional focus, percentage of height
                (height.toFloat() - headerHeight - weekDaysHeaderRowTotalPadding - spaceBelowAllDayEvents) * zoomFocusPoint
            } else {
                // Grab focus
                detector.focusY
            }

            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scale = detector.scaleFactor

            mNewHourHeight = Math.round(hourHeight * scale)

            // Calculating difference
            var diffY = mFocusedPointY - mCurrentOrigin.y
            // Scaling difference
            diffY = diffY * scale - diffY
            // Updating week view origin
            mCurrentOrigin.y -= diffY

            invalidate()
            return true
        }

    }

    private inner class DragListener : View.OnDragListener {
        override fun onDrag(v: View, e: DragEvent): Boolean {
            when (e.action) {
                DragEvent.ACTION_DROP -> {
                    val headerTitleAndSubtitleTextHeight = headerWeekDayTitleTextHeight + (if (isSubtitleHeaderEnabled) headerWeekDaySubtitleTextHeight + spaceBetweenHeaderWeekDayTitleAndSubtitle else 0.0f)
                    if (e.x > mHeaderColumnWidth && e.y > headerTitleAndSubtitleTextHeight + weekDaysHeaderRowTotalPadding + spaceBelowAllDayEvents) {
                        val selectedTime = getTimeFromPoint(e.x, e.y)
                        if (selectedTime != null) {
                            dropListener!!.onDrop(v, selectedTime)
                        }
                    }
                }
            }
            return true
        }
    }

//endregion interfaces

    companion object {
        @Deprecated("")
        val LENGTH_SHORT = 1
        @Deprecated("")
        val LENGTH_LONG = 2
    }
}
