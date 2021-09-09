package constants

object Constants  {
    /**
     * EN
     * Used to delay closing IncomingCallActivity
     *
     * Situation 1: onResume
     * It may be such a situation that our IncomingCallActivity will be below(under) Android IncomingCallActivity
     * This can happen if the phone is slow and Android IncomingCallActivity appears for a long time
     * In this case, our Activity will be paused and when the user ends the call, our activity will trigger onResume and it will be visible
     * Although the call ended and she is no longer needed
     *
     * Situation 2: onPause
     * The user starts manipulating the Android IncomingCallActivity and our IncomingCallActivity fades into the background(onPause)
     * It can be closed immediately, because the broadcast action EXTRA_STATE_IDLE of the end of the call may not come
     * Or the broadcast action IncomingCallActivity.actionCloseActivity may not work
     *
     * It is necessary that there is no situation that our IncomingCall Activity remains visible and there is no call
     *
     * RU
     * Используется для задержкт закрытия IncomingCallActivity
     *
     * Ситация 1: onResume
     * Может быть такая ситуация что наша IncomingCallActivity будет снизу(под) Android IncomingCallActivity
     * Это может случиться, если телефон медленный и Android IncomingCallActivity долго появлялся
     * В этом случае наша Activity будет на паузе и когда пользователь завершит звонок, у нашей активити сработает onResume и она будет видна
     * Хотя звонок завершился и она больше не нужна
     *
     * Ситация 2: onPause
     * Пользователь наничает манипуляции с Android IncomingCallActivity и наша IncomingCallActivity уходит на второй план(onPause)
     * Ее можно сразу закрывать, потому что может не прийти broadcast action EXTRA_STATE_IDLE завершения звонка
     * Или может не сработать broadcast action IncomingCallActivity.actionCloseActivity
     *
     * Нужно стараться чтобы не было ситуации, что наша IncomingCall Activity осталась висеть а звонка нет
     */
    const val incomingCallActivityCloseDelay = 1000L


    /**
     * EN
     * A little delay before showing our IncomingCallActivity
     * With an incoming call, if you immediately call our IncomingCallActivity, then it will be below(under) Android IncomingCallActivity
     *
     * RU
     * Небольшая задержка перед тем, как показать нашу IncomingCallActivity
     * При входящем звонке, если сразу вызвать нашу IncomingCallActivity то она будет снизу(под) Android IncomingCallActivity
     */
    const val approximatelyDelayCallerAppAppear = 1000L

    const val url = "https://office-api.bflow.me/"
}