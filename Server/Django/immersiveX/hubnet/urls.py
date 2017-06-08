from django.conf.urls import url

from . import views

'''
	urls.py IE 2.0
	Author: Moreno Colombo
	Creation date: 28.12.2016
	Last modified: 18.01.2017
'''

urlpatterns = [
	## Utilities
	url(r'^serverState', views.server_state, name='server_state'),
    url(r'^version', views.ver, name='version'),
	
	## Tables
	url(r'^tables/(?P<type>\w+)', views.show_table, name='show_table'),
    url(r'^tables/(?P<type>\w+)/(?P<eventId>\d+)', views.show_table, name='show table with eventId'),
	
	url(r'^addRecords', views.add_records, name='add records'), ## TEST PURPOSES ONLY!
	
	## Data visualization
	url(r'^paths/(?P<eventId>\d+)', views.visualize_paths, name='paths with event'),
	url(r'^paths', views.visualize_paths, name='paths'),
	
	url(r'^movements/(?P<eventId>\d+)/(?P<speed>\d+)/(?P<floorId>\d+)', views.visualize_movements, name='movements with event'),
	url(r'^movements/(?P<eventId>\d+)/(?P<speed>\d+)', views.visualize_movements, name='movements with event'),
	url(r'^movements/(?P<eventId>\d+)/(?P<speed>\d+)', views.visualize_movements, name='movements with event'),
	url(r'^movements/(?P<eventId>\d+)', views.visualize_movements, name='movements with event'),
	url(r'^movements', views.visualize_movements, name='movements'),
	
	## Live visualization
	url(r'^eventConfig/(?P<eventId>\d+)', views.event_config, name='event configuration'),
	
	## Recordings
	url(r'^irec', views.input_records, name='input recordings'),
	
	## Participants registration
    url(r'^getEvents', views.get_all_events, name='get events'),
    url(r'^getGroups/(?P<eventId>\d+)', views.get_groups, name='get groups'),
    url(r'^registerParts', views.register_participants, name='register participants'),

	## Admin
	url(r'^saveEvent', views.save_event, name='save event'),
	url(r'^updateEvent', views.update_event, name='update event'),
]