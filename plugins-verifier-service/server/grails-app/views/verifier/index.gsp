<!doctype html>
<html>
<head>
    <title>Welcome to Grails</title>
</head>

<body>

Check plugin against [since; until]: <br/>
<g:uploadForm action="checkPluginAgainstSinceUntilRange" method="post">
    <input type="file" name="pluginFile"/>
    <input type="submit" value="Check plugin"/>
</g:uploadForm>



Check IDE against all compatible plugins: <br/>
<g:uploadForm action="checkIdeWithAllCompatibleUpdates" method="post">
    <input type="file" name="ideFile"/>
    <input type="submit" value="Check ide"/>
</g:uploadForm>

</body>
</html>
