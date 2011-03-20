/**
<p>
Here are the steps you would normally go through in order to recognise a
character that the user draws.
</p>

<h3>Load the data file</h3>

<p>
Load the data file into a {@link com.leafdigital.kanji.KanjiList} object:
</p>

<pre>
KanjiList list = new KanjiList(
    KanjiList.class.getResourceAsStream("strokes-20100823.xml"));
</pre>

<p>
This is an expensive operation, taking approximately 0.2 seconds on a desktop
PC, so make sure you keep the object around rather than reloading it each time.
</p>

<h3>Obtain the user's drawing</h3>

<p>
It is up to you to accept the user's drawing input via whatever means you
wish. For an example, see com.leafdigital.kanji.example.KanjiPanel.
</p>

<p>
Once the user draws some strokes, you will need to do the following:
</p>

<ul>
<li>Create a new (empty) {@link com.leafdigital.kanji.KanjiInfo} object.</li>
<li>For each stroke, create a new {@link com.leafdigital.kanji.InputStroke} object. You will need
to provide four co-ordinates: start X, start Y, end X, and end Y of the stroke
the user drew. You can use any co-ordinate system you like, provided X goes
rightward (X=100 is to the right of X=50) and Y goes downward (Y=100 is below
Y=50). <i>Note: The system does not care how the user drew the stroke. It only
uses the start and end points.</i></li>
<li>Add each {@link com.leafdigital.kanji.InputStroke} object using
{@link com.leafdigital.kanji.KanjiInfo#addStroke(InputStroke)}.</li>
<li>When all the strokes are added, call {@link com.leafdigital.kanji.KanjiInfo#finish()}; this scales
the input data and prepares data structures ready for matching.</li>
</ul>

<p>
You now have a finished {@link com.leafdigital.kanji.KanjiInfo} object representing the user's
drawing.
</p>

<h3>Compare it against the database</h3>

<p>
To compare and rank the user's drawing, use 
{@link com.leafdigital.kanji.KanjiList#getTopMatches(KanjiInfo, KanjiInfo.MatchAlgorithm)}
to obtain matches.
</p>

<p>
You need to specify the match algorithm to use. There are two basic
algorithms:
</p>

<ul>
<li>Exact match is quite reliable and requires very little CPU time,
but it will only match if the user draws exactly the right number of strokes
and in the right order.</li>
<li>Fuzzy match ignores stroke order and works differently; it requires
vastly more CPU time.</li>
<li>There are three variants of fuzzy match: exact stroke count and also
plus/minus 1 or 2 strokes. Note that these variants are mutually exclusive; 
for example, the plus/minus 1 won't return results that actually have the
correct stroke count.</li>
</ul>

<h3>Process results</h3>

<p>
The
{@link com.leafdigital.kanji.KanjiList#getTopMatches(KanjiInfo, KanjiInfo.MatchAlgorithm)}
function returns a number of {@link com.leafdigital.kanji.KanjiMatch} objects, in order. Each
one contains information about the matched kanji and the match percentage.
</p> 

<p>
You probably want to present these to the user for selection. Depending on
user choice, perhaps you'll then need to run a different match algorithm.
Otherwise, if the user selects a matched kanji, you now have your answer.
</p>

<h3>Report statistics (optional)</h3>

<p>
If you want to contribute to statistics which might allow this library to
be improved in future, then once the user has selected a correct kanji,
you can call the function
{@link com.leafdigital.kanji.StatsReporter#phoneHome(KanjiInfo, String, KanjiInfo.MatchAlgorithm, int, String, StatsReporter.Callback)}
which passes information about the user's drawing to the database at
live.leafdigital.com (if it happens to be available at the time).
</p>

<p>
For privacy reasons, it is a good idea to let users opt into
this facility. Please be ready to upgrade to a new library
version if requested (in case the URL has to change).
</p>
*/
package com.leafdigital.kanji;