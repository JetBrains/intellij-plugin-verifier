/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.output.html

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.output.BaseOutputPrintTest
import org.junit.Before
import org.junit.Test

class HtmlOutputPrintTest : BaseOutputPrintTest<HtmlResultPrinter>() {
  @Before
  override fun setUp() {
    super.setUp()
    resultPrinter = HtmlResultPrinter(verificationTarget, out)
  }

  @Test
  fun `plugin is compatible`() {
    `when plugin is compatible` {
      val expected = HTML_HEADER + """
          <h2>232.0</h2>
          <div class="plugin pluginOk">
            <h3><span class="pMarker">    </span>pluginId</h3>
            <div>
              <div class="update updateOk">
                <h3><span class="uMarker">    </span>1.0<small>pluginId 1.0</small><small>Compatible</small></h3>
                <div>
                  <div class="shortDescription">
                    Dependencies used on verification <a href="#" class="detailsLink">details</a>
                    <div class="longDescription">
                      <pre>
      pluginId:1.0</pre>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
      """.trimIndent() + HTML_FOOTER
      assertOutput(expected)
    }
  }

  @Test
  fun `multiple compatible plugins`() {
    resultPrinter.printResults(
      listOf(
        PluginVerificationResult.Verified(mockPluginInfo("id1"), verificationTarget, dependenciesGraph),
        PluginVerificationResult.Verified(mockPluginInfo("id2"), verificationTarget, dependenciesGraph),
      )
    )

    val expected = HTML_HEADER + """
          <h2>232.0</h2>
          <div class="plugin pluginOk">
            <h3><span class="pMarker">    </span>id1</h3>
            <div>
              <div class="update updateOk">
                <h3><span class="uMarker">    </span>1.0<small>id1 1.0</small><small>Compatible</small></h3>
                <div>
                  <div class="shortDescription">
                    Dependencies used on verification <a href="#" class="detailsLink">details</a>
                    <div class="longDescription">
                      <pre>
      pluginId:1.0</pre>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="plugin pluginOk">
            <h3><span class="pMarker">    </span>id2</h3>
            <div>
              <div class="update updateOk">
                <h3><span class="uMarker">    </span>1.0<small>id2 1.0</small><small>Compatible</small></h3>
                <div>
                  <div class="shortDescription">
                    Dependencies used on verification <a href="#" class="detailsLink">details</a>
                    <div class="longDescription">
                      <pre>
      pluginId:1.0</pre>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
      """.trimIndent() + HTML_FOOTER

    assertOutput(expected)
  }

  @Test
  fun `plugin is dynamic and has structural warnings`() {
    `when plugin is dynamic and has structural warnings` {
      val expected = HTML_HEADER + """
              <h2>232.0</h2>
              <div class="plugin pluginOk">
                <h3><span class="pMarker">    </span>pluginId</h3>
                <div>
                  <div class="update updateOk">
                    <h3><span class="uMarker">    </span>1.0<small>pluginId 1.0</small><small>Compatible. 1 plugin configuration defect</small></h3>
                    <div>
                      <div class="shortDescription">
                        Plugin structure defects <a href="#" class="detailsLink">details</a>
                        <div class="longDescription">
                          <ul>
                            <li>Invalid plugin descriptor 'plugin.xml'. The plugin configuration file does not include any module dependency tags. So, the plugin is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA. Please note that plugins should declare a dependency on `com.intellij.modules.platform` to indicate dependence on shared functionality.</li>
                          </ul>
                        </div>
                      </div>
                      <div class="shortDescription">
                        Dependencies used on verification <a href="#" class="detailsLink">details</a>
                        <div class="longDescription">
                          <pre>
          pluginId:1.0</pre>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
      """.trimIndent() + HTML_FOOTER
      assertOutput(expected)
    }
  }
}

private val HTML_HEADER = """
          <html>
            <head>
              <title>Verification result 232.0</title>
              <script src="https://ajax.aspnetcdn.com/ajax/jQuery/jquery-1.9.1.min.js" type="text/javascript">
              </script>
              <script src="https://code.jquery.com/ui/1.9.2/jquery-ui.min.js" type="text/javascript">
              </script>
              <link rel="stylesheet" href="https://code.jquery.com/ui/1.9.2/themes/base/jquery-ui.css" type="text/css"/>
              <style type="text/css">
          .shortDescription {
              padding: 2px;
          }

          .updates div div:nth-child(odd) {
              background: #eee;
          }

          .uMarker, .pMarker {
              white-space: pre;
              font-weight: bold;
          }

          .updateOk .uMarker {
              background: #0f0;
              color: #0f0;
          }

          .updateHasProblems .uMarker {
              background: #f00;
              color: #f00;
          }

          .warnings .uMarker {
              background: #ffeb00;
              color: #ffeb00;
          }

          .missingDeps .uMarker {
              background: #7fd8ff;
              color: #7fd8ff;
          }

          .badPlugin .uMarker {
              background: #5f3103;
              color: #5f3103;
          }

          .notFound .uMarker {
              background: #e5c2ef;
              color: #e5c2ef;
          }

          .failedToDownload .uMarker {
              background: #ffa87a;
              color: #ffa87a;
          }

          .excluded .uMarker {
              background: #888 !important;
          }

          .problematicOnly .excluded, .problematicOnly .updateOk, .problematicOnly .pluginOk {
              display: none;
          }

          .pluginOk .pMarker {
              background: #0f0;
              color: #0f0;
          }

          .pluginHasProblems .pMarker {
              background: #f00;
              color: #f00;
          }

          .warnings .pMarker {
              background: #ffeb00;
              color: #ffeb00;
          }

          .missingDeps .pMarker {
              background: #7fd8ff;
              color: #7fd8ff;
          }

          .badPlugin .pMarker {
              background: #5f3103;
              color: #5f3103;
          }

          .notFound .pMarker {
              background: #e5c2ef;
              color: #e5c2ef;
          }

          .failedToDownload .pMarker {
              background: #ffa87a;
              color: #ffa87a;
          }

          .shortDescription a {
              color: #2B587A !important;
          }

          .longDescription {
              display: none;
              margin-left: 100px;
              padding: 2px;
          }
              </style>
            </head>
            <body>
          
""".trimIndent()
private val HTML_FOOTER = """
       
              <script>
          //$( "#tabs" ).tabs();
          $(".plugin").accordion({active: false, collapsible: true, heightStyle: 'content'});
          $(".update").accordion({active: false, collapsible: true, heightStyle: 'content'});

          $(".detailsLink").click(function () {
              var longDiv = $(this).parent().find(".longDescription");

              if (longDiv.css('display') !== 'block') {
                  longDiv.css('display', 'block')
              } else {
                  longDiv.css('display', 'none')
              }

              return false
          });

          $(".updateHasProblems .uMarker").attr('title', "Problems found");
          $(".excluded .uMarker").attr('title', "Excluded");

          $(".pluginHasProblem .pMarker").attr('title', "Problems found");
          $(".pluginOk .pMarker").attr('title', "Excluded");
              </script>
            </body>
          </html>

""".trimIndent()
