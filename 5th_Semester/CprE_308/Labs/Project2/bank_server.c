#include <stdio.h>
#include<string.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/time.h>
#include <pthread.h> 


typedef enum{
    ERROR = 0,          //Job init failure sentinel value
    CHECKJOB = 1,
    TRANSJOB = 2,
    ENDJOB = 3
} JobType;


typedef struct CheckJob {
    int account_ID;
} CheckJob;

typedef struct Trans{
    int account_ID;     //ID of account to operate on
    int amount;         //Amount to add or remove from account balance
} Trans;
typedef struct TransJob {
    struct Trans *trans_list[10];//List of individual transaction
    int num_trans;              //Number of transactions in list
} TransJob;


typedef struct Job {
    int request_ID;             //Global ID
    JobType job_type;           //Is this a CheckJob, TransJob, ...?
    struct timeval time_arrival;//Time job is created
    struct timeval time_end;    //Time job is completed

    CheckJob *check_job;        //If this job is a CheckJob, this holds info
    TransJob *trans_job;        //If this job is a TransJob, this holds info

    struct Job *next;
} Job;


typedef struct Queue {
    Job *head;
    Job *tail;
} Queue;

Job newJob(JobType job_type);
struct CheckJob newCheckJob(int account_ID);
struct TransJob newTransJob();
int addTransToJob(struct TransJob transJob, struct Trans transaction);
struct Trans newTransaction(int account_ID, int amount);

int isQueueEmpty();
int enQueue(Job job);
Job popQueue();
void swapJobs(Job *job1, Job *job2);

void *threadDoJobs(void *arg);


Queue *QUEUE;
int REQUESTNUM = 0;

pthread_mutex_t writer_lock;
pthread_mutex_t reader_lock;

int numThreads;
pthread_t *tid;

int numAccounts;
pthread_mutex_t *accLocks;

char *outputFile;


int main(int argc, char *argv[])
{
    printf("Hello world\n");

    if(argc != 4){
        printf("Incorrect number of input parameters: 4 required\n");
        return 1;
    }

    //----------------------------------------------------------------------------
    //                                 Initialization 
    //----------------------------------------------------------------------------

    //Initialize threads
    numThreads = atoi(argv[1]);
    tid = malloc (numThreads * sizeof (pthread_t)); //Holds all thread IDs

    for(int i = 0; i < numThreads; i++){
        int error = pthread_create(&(tid[i]), NULL, &threadDoJobs, "blarg"); 
        if (error != 0) 
            printf("\nThread can't be created :[%s]", strerror(error)); 
        
        pthread_join(tid[i], NULL);
    }


    //Initialize account locks
    numAccounts = atoi(argv[2]);
    accLocks = malloc (numAccounts * sizeof (pthread_mutex_t));

    for(int i = 0; i < numAccounts; i++){
        int error = pthread_mutex_init(&(accLocks[i]), NULL); 
        if (error != 0) 
            printf("\nMutex can't be created :[%s]", strerror(error)); 
    }



    //Initialize queue
    QUEUE = (struct Queue*)malloc(sizeof(struct Queue));
    if (QUEUE == NULL) { 
        printf("Queue not allocated.\n"); 
        return 1;
    }

    //Initialize the mutexes
    if (pthread_mutex_init(&writer_lock, NULL) != 0) { 
        printf("\n Writer mutex init has failed!\n"); 
        return 1; 
    }
    if (pthread_mutex_init(&reader_lock, NULL) != 0) { 
        printf("\n Reader mutex init has failed!\n"); 
        return 1; 
    }




    //----------------------------------------------------------------------------
    //                                  Begin Loop 
    //----------------------------------------------------------------------------

    char line[1024];
    while(1){
        printf("> ");                           //Ask for user input

        char *str = fgets(line, 1024, stdin);   //Grab user input
        if (!str){
            printf("Couldn't read user input\n");
            continue;
        }
        str[strcspn(str, "\n")] = 0;            //Remove the newline char

        printf("String: %s\n", str);


        //Convert input into a job
        Job job = stringToJob(str);

        //Add job to queue

        //If the job is an exit job, break loop
        break;
    }


    //----------------------------------------------------------------------------
    //                                 Dissasembly 
    //----------------------------------------------------------------------------


    //Reconnect with all threads
    for(int i = 0; i < numThreads; i++){
        pthread_join(tid[i], NULL); 
    }

    //Destroy mutexes
    pthread_mutex_destroy(&writer_lock);
    pthread_mutex_destroy(&reader_lock); 

    


    free(QUEUE);    //Free Queue
    free(tid);      //Free thread array
    free(accLocks); //Free account mutex locks array

    return 0;
}


int isQueueEmpty(){
    return (QUEUE->head == QUEUE->tail);
}
int enQueue(Job job){
    //Job *temp = &newJob(CHECKJOB);
    return 0;
}
Job popQueue(){
    Job temp = newJob(CHECKJOB);
    return temp;
}

//Swap the pointers of two jobs, effectively swapping contents
void swapJobs(Job *job1, Job *job2){
    Job temp = *job1;
    *job1 = *job2;
    *job2 = temp;
}




Job newJob(JobType job_type){
    struct Job temp = {
        .request_ID = REQUESTNUM++, //Global request number (increment it after)
        .job_type = job_type
    };
    gettimeofday(&(temp.time_arrival), NULL);

    return temp;
}

CheckJob newCheckJob(int account_ID){
    CheckJob temp = {
        .account_ID = account_ID
    };
    return temp;
}

TransJob newTransJob(){
    TransJob transJob = {
        .num_trans = 0
    };
    return transJob;
}
int addTransToJob(TransJob transJob, Trans transaction){
    int index = transJob.num_trans;
    transJob.trans_list[index] = &transaction;
    transJob.num_trans = transJob.num_trans + 1;

    return index;
}
Trans newTransaction(int account_ID, int amount){
    Trans trans = {
        .account_ID = account_ID,
        .amount = amount
    };
    return trans;
}


void *threadDoJobs(void *arg){
    printf("Thread created\n");
    
    return NULL;
}

Job stringToJob(char *str){
    char s = ' ';       //Split command on space
    char *jobType = strtok(str, s);


    if(!strcmp("CHECK", jobType)){
        int accID = atoi(strtok(str, s));

        CheckJob checkJob = newCheckJob(accID);
        Job job = newJob(CHECKJOB);

        job.check_job = &checkJob;
        return job;
    }


    else if(!strcmp("TRANS", jobType)){
        TransJob transJob = newTransJob();

        char *id = "";
        char *amount = "";
        int count = 0;
        while(count < 10){
            //Get the information if we haven't reached the end
            id = strtok(str, s);
            if(!id) break;

            amount = strtok(str, s);
            if(!amount) {           //Uneven #, need to come in pairs
                printf("Invalid number of arguments\n");
                return newJob(ERROR);
            }

            //Make the transaction
            Trans trans = newTransaction(atoi(id), atoi(amount));

            //Add to the list
            transJob.trans_list[count++] = &trans;
            transJob.num_trans = count;
        }

        //Make the main job
        Job job = newJob(TRANSJOB);
        job.trans_job = &transJob;
        return job;
    }

    else if(!strcmp("EXIT", jobType)){
        
    }
    else{
        printf("< Invalid command %s\n", jobType);
        return newJob(ERROR);
    }

    //Grab first word


    //Turn string into stuff
    char commands[20];
    for(int i = 0; i < 20; i++){

    }
}



/*
int addToQueue(struct Job job){
    printf("AAAA: %d\n", job.request_ID);
    printf("Urg0: %d\n", QUEUE->tail->request_ID);
    swapJobs(&job, QUEUE->tail);
    printf("Urg1: %d\n", QUEUE->tail->request_ID);

    QUEUE->tail->next = &job;   //Job is now the old tail
    printf("Urg2: %d\n", QUEUE->tail->request_ID);
    Job jobTail = {
        .request_ID = -1,
        .job_type = TAILNODE
    };
    QUEUE->tail = &jobTail;
    printf("Urg3: %d\n", QUEUE->tail->request_ID);
    return 0;
}
*/



void thing(){
    Job job1 = {
        .request_ID = 1,
        .job_type = CHECKJOB
    };
    Job job2 = {
        .request_ID = 2,
        .job_type = CHECKJOB
    };
    Job job3 = {
        .request_ID = 3,
        .job_type = CHECKJOB
    };

    QUEUE->head = &job1;
    QUEUE->tail = &job1;
    printf("%d\n", isQueueEmpty());

    QUEUE->head->next = &job2;
    QUEUE->head->next->next = &job3;
    QUEUE->tail = &job3;






    int a = QUEUE->head->request_ID;
    printf("%d:", a);
    int b = QUEUE->head->next->request_ID;
    printf("%d:", b);
    int c = QUEUE->head->next->next->request_ID;
    printf("%d\n", c);


    printf("%d\n", isQueueEmpty());
}
void thing2(){
    Job jobTail = {
        .request_ID = -1,
        .job_type = CHECKJOB
    };
    QUEUE->head = &jobTail;
    QUEUE->tail = &jobTail;

    printf("Empty? - %d\n", isQueueEmpty());

    Job job1 = {
        .request_ID = 1,
        .job_type = CHECKJOB
    };
    Job job2 = {
        .request_ID = 2,
        .job_type = CHECKJOB
    };


    enQueue(job1);



    printf("\n-------\n");
    printf("Job1: %d\n", job1.request_ID);
    printf("Job2: %d\n", job2.request_ID);
    printf("-------\n");
    swapJobs(&job1, &job2);
    printf("Job1: %d\n", job1.request_ID);
    printf("Job2: %d\n", job2.request_ID);
    printf("-------\n");
    swapJobs(&job1, &job2);
    printf("Job1: %d\n", job1.request_ID);
    printf("Job2: %d\n", job2.request_ID);
    printf("-------\n\n");
}


/*
int a = job.request_ID;
int b = QUEUE->head->request_ID;
printf("Job1: %d, Job2: %d\n", a, b);

swapJobs(&job, QUEUE->tail);

a = job.request_ID;
b = QUEUE->head->request_ID;
printf("Job1: %d, Job2: %d\n", a, b);

*/

/*
printf("%d\n", isQueueEmpty());

a = QUEUE->head->request_ID;
b = QUEUE->head->next->request_ID;
int c = QUEUE->head->next->next->request_ID;

printf("%d:%d:%d\n", a, b, c);





printf("%d:%d\n", job1.request_ID, job2.request_ID);
swapJobs(&job1, &job2);
printf("%d:%d\n", job1.request_ID, job2.request_ID);
*/

/*
printf("%d:%d\n", job1.request_ID, job2.request_ID);
swapJobs(&job1, &job2);
printf("%d:%d\n", job1.request_ID, job2.request_ID);
*/
