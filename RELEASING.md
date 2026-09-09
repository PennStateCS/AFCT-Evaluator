# Making a release

A release is a version of the evaluator that other people can download. It appears on the
[Releases page](https://github.com/PennStateCS/AFCT-Evaluator/releases), and anyone can download
it without a GitHub account.

You create a release by creating a **tag**, which is a label on one commit that says "this is
version 1.4.0".

A release contains one thing: the jar. The evaluator is run with `java -jar`, so there is nothing
to install.

## Before you start

Make sure your changes are already merged into `main` and the checks passed. Look for the green
tick next to the newest commit on the
[main branch](https://github.com/PennStateCS/AFCT-Evaluator/commits/main).

## Step 1: choose the new version number

Look at `pom.xml` near the top:

```xml
<version>1.4.0</version>
```

The version has three numbers. Pick the new one like this:

- Fixed a bug, nothing else changed: increase the **last** number. 1.4.0 becomes 1.4.1.
- Added something new: increase the **middle** number and set the last to zero. 1.4.0 becomes 1.5.0.
- Changed how the evaluator decides an answer is right or wrong: increase the **first** number.

That last one matters more here than in most projects. Grades depend on this program, so a change
to what it accepts is a bigger deal than a new feature, and the version number should say so.

## Step 2: update the version in `pom.xml`

Edit that line so it holds your new number, then open a pull request with just that change and
merge it once the checks pass.

This step is easy to forget, and it matters. The version number gets written inside the jar
itself, so if you skip it the release would say one thing and contain another. The release refuses
to build if you forget, so nothing bad can happen, but you will have to come back and do it.

## Step 3: get the latest `main` and check it passed

```bash
git checkout main
git pull
```

Then open the [Actions tab](https://github.com/PennStateCS/AFCT-Evaluator/actions) and check that
the newest run on `main` finished with a green tick. If it is still running, wait for it.

## Step 4: create the tag and push it

The tag is the version number with a `v` in front of it. For version 1.4.0 the tag is `v1.4.0`.

```bash
git tag v1.4.0
git push origin v1.4.0
```

That is the whole release. You do not need a pull request for a tag.

## Step 5: watch it build

Open the [Actions tab](https://github.com/PennStateCS/AFCT-Evaluator/actions). A run called
**Release** starts within a few seconds and takes a couple of minutes.

When it finishes, your new release is on the
[Releases page](https://github.com/PennStateCS/AFCT-Evaluator/releases) with the jar attached and
a list of what changed since last time.

## What the checks do and do not prove

There are no tests in this repository, so the build proves the evaluator compiles and packages.
It does not prove the evaluator marks anything correctly.

The check that does that lives in the dashboard repository: `npm run test:evaluator` runs the jar
against known correct and incorrect submissions in `test/evaluator/`. Run it there after building
a new jar, before anyone relies on the release.

## If the release fails

The release checks two things before it builds anything. If either one fails, nothing is
published, so there is nothing to clean up. Open the failed run in the Actions tab and read the
step marked in red.

**"No successful CI run for ... so this tag is not releasable."**

You tagged a commit whose checks never passed, or never ran at all. To fix it:

1. Remove the tag (see below).
2. Make sure the checks pass on `main`.
3. Tag again.

**"Tag v1.5.0 does not match the pom version 1.4.0."**

You skipped step 2, or the number in the tag is not the number in `pom.xml`. To fix it:

1. Remove the tag (see below).
2. Update `<version>` in `pom.xml` to match, through a pull request as usual.
3. Tag again once that is merged.

**"pom.xml asks for afct-client X, but afct-client/afct-client-vX.jar is not in this repository."**

Somebody changed which client version the evaluator builds against without adding that client jar
to the `afct-client` folder. Download the matching jar from the
[AFCT Client releases](https://github.com/PennStateCS/AFCT-Client/releases), add it to that
folder, and merge that before tagging.

### Removing a tag

If you tagged the wrong thing, remove the tag and start again:

```bash
git tag -d v1.4.0
git push origin --delete v1.4.0
```

If a release was already published, delete it on the
[Releases page](https://github.com/PennStateCS/AFCT-Evaluator/releases) as well, or run:

```bash
gh release delete v1.4.0
```

Try not to remove a release other people have already downloaded. If a version turns out to be
broken, it is usually kinder to release a fixed version with a higher number than to make an old
one disappear.

## Test releases

To try a release without it looking like the finished thing, put a dash and a label on the end of
the version. You do **not** need to change `pom.xml` for this: the check only compares the
numbers, so a pom saying 1.4.0 accepts both `v1.4.0` and `v1.4.0-rc1`.

```bash
git tag v1.4.0-rc1
git push origin v1.4.0-rc1
```

Anything with a dash in it is published as a **pre-release**. It appears on the Releases page
marked clearly, and it does not become the version people land on when they visit that page.
