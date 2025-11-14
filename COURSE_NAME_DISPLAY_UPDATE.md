# Recently Opened PDFs - Course Name Display Update

## Change Summary

Updated the "Recently Opened" section on the Home Screen to display the **actual course name** (code + title) instead of just the course ID.

## What Changed

### Before:
Cards showed: `BIO101` (just the course ID)

### After:
Cards show: `BIO101 - Introduction to Biology` (course code + full title)

---

## Files Modified

### 1. HomeViewModel.kt

**Added:**
- `CourseRepository` dependency injection
- `_courseNameMap` state to cache course ID → name mappings
- `loadCourses()` method - loads all user courses on init
- `getCourseName(courseId: String)` public function - returns formatted course name

**How it works:**
```kotlin
// Loads courses and creates mapping
private fun loadCourses() {
    val courses = courseRepository.getUserCourses()
    val mapping = courses.associate { course ->
        course.id to "${course.code} - ${course.title}"
    }
    _courseNameMap.value = mapping
}

// Public function to get course name
fun getCourseName(courseId: String): String {
    return _courseNameMap.value[courseId] ?: courseId  // Falls back to ID if not found
}
```

### 2. HomeScreen.kt

**Updated Recently Opened card creation:**

**Before:**
```kotlin
RecentPdfCard(
    subject = pdf["courseId"] as? String ?: "",  // Just the ID
    ...
)
```

**After:**
```kotlin
val courseId = pdf["courseId"] as? String ?: ""
RecentPdfCard(
    subject = homeViewModel.getCourseName(courseId),  // Full course name
    ...
)
```

---

## Display Format

The course name is displayed as:
- **Format:** `{courseCode} - {courseTitle}`
- **Example:** `CS101 - Introduction to Computer Science`
- **Fallback:** If course not found, shows the original `courseId`

---

## Benefits

✅ **More Informative** - Users see full course name at a glance  
✅ **Better UX** - No need to remember what "BIO101" means  
✅ **Consistent** - Matches course display format used elsewhere  
✅ **Smart Caching** - Course names loaded once, reused for all cards  
✅ **Safe Fallback** - Shows course ID if course can't be found  

---

## Technical Details

- **Loading:** Courses loaded automatically on HomeViewModel init
- **Caching:** Course name map stored in memory (no repeated Firebase calls)
- **Performance:** Map lookup is O(1), very fast
- **Null Safety:** Falls back to courseId if mapping doesn't exist

---

## Example Output

If user recently opened notes from these courses:

| Course ID | Course Name | Card Displays |
|-----------|-------------|---------------|
| BIO101 | Introduction to Biology | `BIO101 - Introduction to Biology` |
| MATH201 | Calculus I | `MATH201 - Calculus I` |
| CS150 | Data Structures | `CS150 - Data Structures` |

---

## Status

✅ **IMPLEMENTED AND TESTED**

The feature now shows meaningful course names instead of cryptic IDs in the Recently Opened section!

