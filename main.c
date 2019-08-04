#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>
#include <semaphore.h>
#include <stdbool.h>

pthread_t P1,P2,P3,P4,P5,P6;

int max_stack_length = 20;
/* *************stack************** */
struct my_stack{
	unsigned int data;
	struct my_stack *next;
};
struct my_stack *top = NULL;
/* *************stack************** */
//stack functions
void add_stack(unsigned int data){
	struct my_stack *tmp;
	tmp = (struct my_stack*)malloc(sizeof(struct my_stack));
	tmp->data = data;
	if(top == NULL){
		tmp->next = NULL;
		top = tmp;
	}else{
		tmp->next = top;
		top = tmp;
	}
}
int get_del_top(){
	struct my_stack *tmp;
	int p;
	if(top != NULL){
		tmp = top;
		top = tmp->next;
		p = (int)tmp->data;
		free(tmp);
	}else{
		p = -1;
	}
	return p;
}
//stack functions
pthread_mutex_t mut_s = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t sig = PTHREAD_COND_INITIALIZER;
pthread_barrier_t bar;
bool f = false;
sem_t sem_s;

int a;
long b;
long long c;

void* P1_producer(void *unuse){
	int sem_val;
	int iter = 0;
	int n = 0;
	while(1){
		if(top == NULL){
			iter++;
		}if(iter == 2){
			break;
		}
		sem_getvalue(&sem_s,&sem_val);
		if(sem_val < max_stack_length){
			pthread_mutex_lock(&mut_s);
			add_stack(n++);
			
			sem_getvalue(&sem_s,&sem_val);
			printf("Producer thread: semaphore=%d; element %d CREATED; \n",
			sem_val,top->data);

            pthread_mutex_unlock(&mut_s);
            sem_post(&sem_s);
		}
	}
	pthread_cancel(P2);
	pthread_cancel(P3);
	pthread_cancel(P4);
	pthread_cancel(P5);
	pthread_cancel(P6);
	printf("P1  stopped !!!\n");
	return NULL;
}

void* P4_consumer(void *unuse){
	int el, val;
	while(1){
		pthread_mutex_lock(&mut_s);
		while(f == false){
			printf("thread_P4 is waiting\n");
			pthread_cond_wait(&sig,&mut_s);
			printf("P4 working after SIG\n");
		}
		pthread_mutex_unlock(&mut_s);
		sem_wait (&sem_s);
		pthread_mutex_lock(&mut_s);
		sem_getvalue(&sem_s,&val);
		if(val >= 0){
			el = get_del_top();
			printf("P4 element: %d, sem: %d\n", el, val);
		}
		pthread_mutex_unlock(&mut_s);
	}
	printf("P4  stopped !!!\n");
	return NULL;
}

void* P5_func(void *unuse){
	int el, val;
	while(1){
		//modif
		if(f == false){
			pthread_mutex_lock(&mut_s);
			f = true;
			pthread_cond_signal(&sig);
			printf("P5 signal_post\n");
			pthread_mutex_unlock(&mut_s);
		}
		sem_wait (&sem_s);
		pthread_mutex_lock(&mut_s);
		sem_getvalue(&sem_s,&val);
		if(val >= 0){
			el = get_del_top();
			printf("P5 element: %d, sem: %d\n", el, val);
		}
		pthread_mutex_unlock(&mut_s);
	}
	printf("P5  stopped !!!\n");
	return NULL;
}

void* P2_func(void *unuse){
	int el, val;
	while(1){
		pthread_mutex_lock(&mut_s);
		while(f == false){
			printf("thread_P2 is waiting\n");
			pthread_cond_wait(&sig,&mut_s);
			printf("P2 working after SIG\n");
		}
		pthread_mutex_unlock(&mut_s);
		//v&m
		sem_wait (&sem_s);
		pthread_mutex_lock(&mut_s);
		sem_getvalue(&sem_s,&val);
		if(val >= 0){
			el = get_del_top();
			printf("P2 element: %d, sem: %d\n", el, val);
		}
		pthread_mutex_unlock(&mut_s);
	}
	printf("P2  stopped !!!\n");
	return NULL;
}

void* P3_func(void *unuse){
	while(1){
		pthread_mutex_lock(&mut_s);
		while(f == false){
			printf("thread_P3 is waiting\n");
			pthread_cond_wait(&sig,&mut_s);
			printf("P3 working after SIG\n");
		}
		pthread_mutex_unlock(&mut_s);
		pthread_barrier_wait(&bar);
		printf("P3 working after bar\n");
		//v
	}
	printf("P3  stopped !!!\n");
	return NULL;
}

void* P6_func(void *unuse){
	while(1){
		pthread_mutex_lock(&mut_s);
		while(f == false){
			printf("thread_P6 is waiting\n");
			pthread_cond_wait(&sig,&mut_s);
			printf("P6 working after SIG\n");
		}
		pthread_mutex_unlock(&mut_s);
		pthread_barrier_wait(&bar);
		printf("P6 working after bar\n");
		//m
	}
	printf("P6  stopped !!!\n");
	return NULL;
}

int main(){
	int sem_value;
	sem_init (&sem_s, 0, 0);
	sem_getvalue(&sem_s,&sem_value);
    printf("semaphore=%d\n",sem_value);
    
	pthread_create(&P1,NULL,P1_producer,NULL);
	pthread_create(&P2,NULL,P2_func	,NULL);
	pthread_create(&P3,NULL,P3_func	,NULL);
	pthread_create(&P4,NULL,P4_consumer,NULL);
	pthread_create(&P5,NULL,P5_func	,NULL);
	pthread_create(&P6,NULL,P6_func	,NULL);
	
	pthread_join(P1,NULL);
	pthread_join(P2,NULL);
	pthread_join(P3,NULL);
	pthread_join(P4,NULL);
	pthread_join(P5,NULL);
	pthread_join(P6,NULL);
	
	printf("ALL STOPED\n");
	
	return 0;
}
