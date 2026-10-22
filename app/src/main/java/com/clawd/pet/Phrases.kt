package com.clawd.pet

object Phrases {
    private val RU = mapOf(
        "hello" to listOf("Привет!", "Я Clawd!", "О, это ты!"),
        "idle" to listOf(
            "Чем помочь?",
            "Хм…",
            "Думаю…",
            "Можно я тут посижу?",
            "Ты молодец!",
            "Не забудь попить воды",
            "Пип-пип!",
            "*оглядывается*",
            "Пишу код… шучу",
        ),
        "tap" to listOf("Ой!", "Эй!", "Чего?", "Я тут!", "Хи!"),
        "annoyed" to listOf("Хватит тыкать!", "Щекотно же!", "Ну всё, обиделся", "Эй, полегче!"),
        "pet" to listOf("Мрр…", "Ещё!", "Приятно ♥", "Хи-хи", "Лучший день!", "♥♥♥"),
        "drag" to listOf("Ааа!", "Куда мы?", "Поставь меня!"),
        "throw" to listOf("Уиии!", "Я лечу!", "Ваааа!"),
        "dizzy" to listOf("Голова кружится…", "@_@", "Где я?"),
        "wake" to listOf("А? Что?", "Я не спал!", "*зевает*"),
        "dream" to listOf(
            "Мне снится море из пикселей…",
            "А во сне я умею летать.",
            "Снится, будто я — большой кот.",
            "Вижу во сне бесконечный понедельник…",
            "Кто-то гладит меня по голове…",
            "Вижу гору вкусных пикселей.",
            "Снится тёплый коврик и много места.",
            "А если я — просто сон?..",
        ),
        "jump" to listOf("Хоп!", "Йе!", "Ура!"),
        "dance" to listOf("Найс бит!", "Оооо, вайб!", "Погнали!", "Ну-ка, потанцуем!", "Это мой трек!"),
        "flee" to listOf("Всё, ухожу!", "Хватит меня тыкать!", "Пока-пока!", "Я больше не хочу!"),
        "sleep" to listOf("Ухожу в сон…", "Спокойной ночи!", "*испаряюсь*"),
        "back" to listOf("Я вернулся!", "Скучал…", "Снова тут!"),
        "wave" to listOf("Привет-привет!", "Помахал!", "Эй, смотри!"),
    )

    private val EN = mapOf(
        "hello" to listOf("Hi!", "I'm Clawd!", "Oh, it's you!"),
        "idle" to listOf(
            "Need help?",
            "Hmm…",
            "Thinking…",
            "Mind if I sit here?",
            "You're doing great!",
            "Don't forget to drink water",
            "Beep-boop!",
            "*looks around*",
            "Writing code… just kidding",
        ),
        "tap" to listOf("Ow!", "Hey!", "What?", "I'm here!", "Hehe!"),
        "annoyed" to listOf("Stop poking me!", "That tickles!", "Fine, I'm offended", "Hey, easy!"),
        "pet" to listOf("Purr…", "More!", "That feels nice ♥", "Hehe", "Best day ever!", "♥♥♥"),
        "drag" to listOf("Whoa!", "Where are we going?", "Put me down!"),
        "throw" to listOf("Wheee!", "I'm flying!", "Ahhh!"),
        "dizzy" to listOf("My head is spinning…", "@_@", "Where am I?"),
        "wake" to listOf("Huh? What?", "I wasn't sleeping!", "*yawns*"),
        "dream" to listOf(
            "I dream of a sea of pixels…",
            "In my dream, I can fly.",
            "I dream I'm a big cat.",
            "Dreaming of an endless Monday…",
            "Someone is petting my head…",
            "I see a mountain of tasty pixels.",
            "Dreaming of a warm rug and lots of space.",
            "What if I'm just a dream?..",
        ),
        "jump" to listOf("Hop!", "Yay!", "Woohoo!"),
        "dance" to listOf("Nice beat!", "This is my jam!", "Let's go!", "Dance time!", "Love this track!"),
        "flee" to listOf("OK, I'm leaving!", "Enough poking me!", "Bye-bye!", "I don't want this!"),
        "sleep" to listOf("Falling asleep…", "Good night!", "*evaporates*"),
        "back" to listOf("I'm back!", "Miss me?", "Here again!"),
        "wave" to listOf("Hi-hi!", "Waved!", "Hey, look!"),
    )

    fun get(lang: String): Map<String, List<String>> = when (lang) {
        "en" -> EN
        else -> RU
    }
}
