  function cutClassName(classTxt) {
    var dotIdx = classTxt.lastIndexOf('.')
    return "<span>" + classTxt.substring(0, dotIdx + 1) + "</span><a href='#' class='className'>" + classTxt.substring(dotIdx + 1) + "</a>"
  }

  function cutClassesInText(text) {
    var classNameFragments = text.match(/([a-zA-Z_$][a-zA-Z_$0-9]+\.)+([A-Z][a-zA-Z_$0-9]+)/g)
    if (classNameFragments == null || classNameFragments.length == 0) {
      return classNameFragments
    }

    var index = 0;
    var res = ""

    for (var i = 0; i < classNameFragments.length; i++) {
      var classFragment = classNameFragments[i]

      var fragmentIdx = text.indexOf(classFragment, index)
      if (fragmentIdx == -1) return text // some error

      res += text.substring(index, fragmentIdx) + cutClassName(classFragment)
      index = fragmentIdx + classFragment.length
    }

    res += text.substring(index)
    return res
  }

  function cutClasses(divs) {
    divs.html(function(index, oldHtml) {
      return cutClassesInText(oldHtml)
    })

    divs.find('.className').each(function() {
      var packageSpan = $(this.previousSibling)
      packageSpan.css('display', 'none')

      var a = $(this)

      a.attr('title', packageSpan.text() + a.text())

      a.click(function() {
        var prev = $(this.previousSibling)
        if (prev.css('display') == 'none') {
          prev.show()
        }
        else {
          prev.hide()
        }

        return false
      })
    })
  }

  //$( "#tabs" ).tabs();
  $( ".plugin" ).accordion({active: false, collapsible: true, heightStyle: 'content'})
  $( ".update" ).accordion({active: false, collapsible: true, heightStyle: 'content'})

  cutClasses($(".errorDetails"))
  cutClasses($(".errLoc"))

  $(".detailsLink").click(function() {
    var locDiv = $(this).parent().find(".errLoc")

    if (locDiv.css('display') != 'block') {
      locDiv.css('display', 'block')
    }
    else {
      locDiv.css('display', 'none')
    }

    return false
  })

  $(".updateHasProblems .uMarker").attr('title', "Problems found")
  $(".excluded .uMarker").attr('title', "Excluded")

  $(".pluginHasProblem .pMarker").attr('title', "Problems found")
  $(".pluginOk .pMarker").attr('title', "Excluded")
