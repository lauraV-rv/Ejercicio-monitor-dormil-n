import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Monitor implements Runnable{
    private Semaphore monitorSemaphore;
    private Lock lock;
    private Queue<Integer> waitingStudents;
    private boolean isSleeping = true; 

    public Monitor(Semaphore monitorSemaphore, Lock lock, Queue<Integer> waitingStudents){
        this.monitorSemaphore = monitorSemaphore;
        this.lock = lock;
        this.waitingStudents = waitingStudents;
    }

    @Override
    public void run(){
        while(true){
            try{
                if (isSleeping) {
                    System.out.println("El monitor está durmiendo");
                }
                monitorSemaphore.acquire(); 

                while (true){
                    Integer studentId;
                    lock.lock();
                    try{
                        if (waitingStudents.isEmpty()){
                            isSleeping = true;
                            break;
                        }
                        studentId = waitingStudents.poll();
                    } finally {
                        lock.unlock();
                    }

                    isSleeping = false;
                    System.out.println("El monitor está ayudando al estudiante " + studentId);
                    Thread.sleep(new Random().nextInt(3000) + 1000);
                    System.out.println("El monitor ha terminado de ayudar al estudiante " + studentId);
                }
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
                System.out.println("Monitor interrumpido");
                break;
            }
        }
    }
    
}

class Estudiante implements Runnable{
    private int id;
    private Semaphore monitorSemaphore;
    private Lock lock;
    private Queue<Integer> waitingStudents;
    private int maxChairs;
    private Random rand;

    public Estudiante(int id, Semaphore monitorSemaphore, Lock lock, Queue<Integer> waitingStudents, int maxChairs){
        this.id = id;
        this.monitorSemaphore = monitorSemaphore;
        this.lock = lock;
        this.waitingStudents = waitingStudents;
        this.maxChairs = maxChairs;
        this.rand = new Random();
    }

    @Override
    public void run(){
        while(true){
            try{
                System.out.println("El estudiante " + id + " está programando");
                Thread.sleep(rand.nextInt(4000) + 2000); 

                boolean canSit = false;
                lock.lock();
                try{
                    if (waitingStudents.size() < maxChairs){
                        waitingStudents.add(id);
                        canSit = true;
                        System.out.println("El estudiante " + id + " está esperando en una silla " +
                                waitingStudents.size() + " estudiantes en la fila");
                    } else{
                        System.out.println("El estudiante " + id + " no encuentra silla y se va");
                    }
                } finally{
                    lock.unlock();
                }

                if (canSit){
                    lock.lock();
                    try {
                        if (waitingStudents.size() == 1){ 
                            System.out.println("El estudiante " + id + " despierta al monitor");
                            monitorSemaphore.release();
                        }
                    } finally {
                        lock.unlock();
                    }
                }

                Thread.sleep(rand.nextInt(5000) + 3000); 
            } catch(InterruptedException e){
                Thread.currentThread().interrupt();
                System.out.println("El estudiante " + id + " interrumpido");
                break;
            }
        }
    }
}

public class MonitorEstudiantes{
    public static void main(String[] args){
        int numEstudiantes = 5;
        int maxChairs = 3;

        Semaphore monitorSemaphore = new Semaphore(0);
        Lock lock = new ReentrantLock();
        Queue<Integer> waitingStudents = new LinkedList<>();

        Thread monitor = new Thread(new Monitor(monitorSemaphore, lock, waitingStudents));
        monitor.start();

        for (int i = 1; i <= numEstudiantes; i++){
            new Thread(new Estudiante(i, monitorSemaphore, lock, waitingStudents, maxChairs)).start();
        }
    }
}
