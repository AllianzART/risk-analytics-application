eventTestCaseStart = { name ->
    println '-' * 60
    println "|$name : started"
}

eventTestCaseEnd = { name, err, out ->
    println "\n|$name : finished"
}