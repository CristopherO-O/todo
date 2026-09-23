package src;
import java.util.ArrayList;
import java.util.List;

// ===== TASKS =====
class Task {
    int id;
    int priority;
    String title;
    String description;
    boolean done;

    public Task(int id, int priority, String title, String description){
        this.id = id;
        this.priority = priority;
        this.title = title;
        this.description = description;
        this.done = false;
    }

    public void setTaskDone(){
        this.done = true;
    }
}

// ===== AGENDA =====
class Agenda {
    int id;
    boolean remind;
    String title;
    String date;
    String time;

    public Agenda(int id, String title, String date, String time, boolean remind){
        this.id = id;
        this.title = title;
        this.date = date;
        this.time = time;
        this.remind = remind;
    }
}

// ===== NOTES =====
class Notes {
    int id;
    String title;
    String content;

    public Notes(int id, String title, String content){
        this.id = id;
        this.title = title;
        this.content = content;
    }
}

// ===== WishList =====
class WishList {
    int id;
    String title;
    String content;
    boolean acquired;

    public WishList(int id, String title, String content){
        this.id = id;
        this.title = title;
        this.content = content;
        this.acquired = false;
    }

    public void setAcquired(){
        this.acquired = true;
    }
}

// ========== SYSTEM ==========
class DataSystem {
    List<Task> tasks = new ArrayList<>();
    List<Agenda> agenda = new ArrayList<>();
    List<Notes> notes = new ArrayList<>();
    List<WishList> wishList = new ArrayList<>();

    private int taskId = 0;
    private int agendaId = 0;
    private int notesId = 0;
    private int wishListId = 0;

    // ===== SYNC IDS =====
    public void syncIds(){
        taskId     = tasks.stream()   .mapToInt(t -> t.id).max().orElse(-1) + 1;
        agendaId   = agenda.stream()  .mapToInt(a -> a.id).max().orElse(-1) + 1;
        notesId    = notes.stream()   .mapToInt(n -> n.id).max().orElse(-1) + 1;
        wishListId = wishList.stream().mapToInt(w -> w.id).max().orElse(-1) + 1;
    }

    // ===== TASK =====
    public void addTask(int priority,String title, String content){
        tasks.add(new Task(taskId++, priority, title, content));
    }

    public Task getTask(int id){
        return tasks.stream().filter(t -> t.id == id).findFirst().orElse(null);
    }

    public void completeTask(int id){
        Task t = getTask(id);
        if(t != null) t.setTaskDone();
    }

    public void sortTasksByPriority(){
        tasks.sort((a, b) -> Integer.compare(a.priority, b.priority));
    }

    public boolean rmTask(int id){
        return tasks.removeIf(t -> t.id == id);
    }

    // ===== AGENDA =====
    public void addAgenda(String title, String date, String time, boolean remind){
        agenda.add(new Agenda(agendaId++, title, date, time, remind));
    }

    public boolean rmAgenda(int id){
        return agenda.removeIf(a -> a.id == id);
    }

    public void sortAgenda(){
        agenda.sort((a, b) -> {
            int cmp = a.date.compareTo(b.date);
            if(cmp != 0) return cmp;
            return a.time.compareTo(b.time);
        });
    }

    // ===== NOTES =====
    public void addNotes(String title, String content){
        notes.add(new Notes(notesId++, title, content));
    }

    public Notes getNote(int id){
        return notes.stream().filter(n -> n.id == id).findFirst().orElse(null);
    }

    public boolean rmNotes(int id){
        return notes.removeIf(n -> n.id == id);
    }

    // ===== WISHLIST =====
    public void addWishList(String title, String content){
        wishList.add(new WishList(wishListId++, title, content));
    }

    public WishList getWishItem(int id){
        return wishList.stream().filter(w -> w.id == id).findFirst().orElse(null);
    }

    public void markWishAsAcquired(int id){
        wishList.stream()
            .filter(w -> w.id == id)
            .findFirst()
            .ifPresent(WishList::setAcquired);
    }

    public boolean rmWishList(int id){
        return wishList.removeIf(w -> w.id == id);
    }

}